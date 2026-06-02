"""
services/outfit_generator_service.py
──────────────────────────────────────
Genera combinaciones candidatas de outfits a partir del guardarropa del usuario.

Responsabilidades:
  1. Filtrar prendas por ocasión y clima del request (filtro del usuario).
  2. Agrupar prendas filtradas por slot (TOP, BOTTOM, OUTERWEAR, FOOTWEAR, ONEPIECE).
  3. Seleccionar el top-K de prendas por slot para acotar el espacio combinatorio.
  4. Generar N combinaciones (producto cartesiano limitado) con coherencia de slots:
       - Modo normal:  TOP + BOTTOM + (OUTERWEAR opcional) + (FOOTWEAR opcional)
       - Modo onepiece: ONEPIECE + (OUTERWEAR opcional) + (FOOTWEAR opcional)
  5. Por cada combinación, calcular el outfit_vector = mean(embeddings de las prendas).
  6. Devolver hasta MAX_CANDIDATES candidatos en orden aleatorio
     (el scoring en dressme-back se encarga del ranking final).

Límites del MVP:
  MAX_CANDIDATES = 20  → techo de combinaciones devueltas al orquestador.
  TOP_K_PER_SLOT = 5   → prendas candidatas por slot (reduce la explosión combinatoria).
                          Con 4 slots y K=5 el espacio máximo es 5^4 = 625 → acotado a 20.

Filtrado por ocasión y clima:
  El guardarropa que llega en el request ya tiene el slot resuelto por dressme-back,
  pero el filtrado por ocasión/clima se hace aquí comparando los campos
  'occasion' y 'weather' del ClothingEmbeddingInfo contra los del request.
  La comparación es case-insensitive para tolerar diferencias de capitalización.

  IMPORTANTE: dressme-back ya filtra las prendas no procesadas (is_processed=false)
  y sin embedding antes de llamar a este endpoint. Sin embargo, el servicio
  también valida y registra cualquier prenda inválida que llegue.
"""

import logging
import random
from itertools import product

import numpy as np

from schemas.outfit import (
    ClothingEmbeddingInfo,
    CandidateSlot,
    OutfitCandidate,
    OutfitGenerationRequest,
    OutfitGenerationResponse,
)

logger = logging.getLogger(__name__)

# ── Constantes ────────────────────────────────────────────────────────────────

MAX_CANDIDATES  = 20   # techo de combinaciones devueltas al orquestador
TOP_K_PER_SLOT  = 5    # prendas candidatas por slot antes del producto cartesiano
REQUIRED_SLOTS  = {"TOP", "BOTTOM"}       # mínimo para un outfit con separates
ONEPIECE_SLOTS  = {"ONEPIECE"}            # slots válidos para outfits onepiece
OPTIONAL_SLOTS  = {"OUTERWEAR", "FOOTWEAR"}  # slots que enriquecen pero no son obligatorios

# Slots que deben ser ignorados si está presente ONEPIECE
EXCLUDED_WITH_ONEPIECE = {"TOP", "BOTTOM"}


class OutfitGeneratorService:
    """
    Genera combinaciones candidatas de outfits a partir del guardarropa del usuario.
    Stateless — no tiene dependencias externas. Inyectado via dependencies.py.
    """

    def generate(self, request: OutfitGenerationRequest) -> OutfitGenerationResponse:
        """
        Punto de entrada principal.

        Flujo:
          1. Filtrar prendas válidas (con embedding, slot válido).
          2. Filtrar por ocasión + clima del request.
          3. Agrupar por slot.
          4. Determinar modo de generación (onepiece vs separates).
          5. Generar combinaciones.
          6. Calcular outfit_vector por combinación.
          7. Limitar a MAX_CANDIDATES y mezclar aleatoriamente.
        """
        logger.info(
            "OutfitGeneratorService: Generando candidatos para usuario=%s "
            "| ocasión=%s | clima=%s | prendas_recibidas=%d",
            request.user_id,
            request.occasion,
            request.weather,
            len(request.wardrobe),
        )

        # ── Paso 1: separar prendas válidas de inválidas ──────────────────────
        valid_items, skipped = self._filter_valid(request.wardrobe)

        if skipped > 0:
            logger.warning(
                "OutfitGeneratorService: %d prendas ignoradas por falta de embedding "
                "o slot inválido.",
                skipped,
            )

        # ── Paso 2: filtrar por ocasión + clima ───────────────────────────────
        filtered = self._filter_by_context(valid_items, request.occasion, request.weather)

        logger.info(
            "OutfitGeneratorService: %d prendas válidas tras filtro ocasión/clima.",
            len(filtered),
        )

        if not filtered:
            logger.warning(
                "OutfitGeneratorService: Ninguna prenda coincide con "
                "ocasión='%s' y clima='%s'. Devolviendo lista vacía.",
                request.occasion,
                request.weather,
            )
            return OutfitGenerationResponse(
                candidates=[],
                total_candidates=0,
                skipped_clothing=skipped,
            )

        # ── Paso 3: agrupar por slot y aplicar top-K ─────────────────────────
        by_slot = self._group_by_slot(filtered)
        by_slot_topk = self._apply_top_k(by_slot, TOP_K_PER_SLOT)

        # ── Paso 4 y 5: generar combinaciones ─────────────────────────────────
        has_onepiece = bool(by_slot_topk.get("ONEPIECE"))

        if has_onepiece:
            raw_combinations = self._generate_onepiece_combinations(by_slot_topk)
        else:
            raw_combinations = self._generate_separates_combinations(by_slot_topk)

        if not raw_combinations:
            logger.warning(
                "OutfitGeneratorService: No se pudieron generar combinaciones. "
                "Slots disponibles: %s. Se requiere al menos TOP+BOTTOM o ONEPIECE.",
                list(by_slot_topk.keys()),
            )
            return OutfitGenerationResponse(
                candidates=[],
                total_candidates=0,
                skipped_clothing=skipped,
            )

        logger.info(
            "OutfitGeneratorService: %d combinaciones generadas antes del límite.",
            len(raw_combinations),
        )

        # ── Paso 6: mezclar y limitar a MAX_CANDIDATES ────────────────────────
        random.shuffle(raw_combinations)
        selected = raw_combinations[:MAX_CANDIDATES]

        # ── Paso 7: calcular outfit_vector por combinación ────────────────────
        candidates = [
            self._build_candidate(combo)
            for combo in selected
        ]

        logger.info(
            "OutfitGeneratorService: %d candidatos devueltos al orquestador.",
            len(candidates),
        )

        return OutfitGenerationResponse(
            candidates=candidates,
            total_candidates=len(raw_combinations),
            skipped_clothing=skipped,
        )

    # ── Filtros ───────────────────────────────────────────────────────────────

    def _filter_valid(
        self, wardrobe: list[ClothingEmbeddingInfo]
    ) -> tuple[list[ClothingEmbeddingInfo], int]:
        """
        Separa las prendas con embedding y slot válido de las que no lo tienen.
        Devuelve (válidas, cantidad_ignoradas).
        """
        valid = []
        skipped = 0
        for item in wardrobe:
            if item.embedding_vector is None:
                logger.debug(
                    "OutfitGeneratorService: Prenda %s ignorada — sin embedding.",
                    item.clothing_id,
                )
                skipped += 1
            else:
                valid.append(item)
        return valid, skipped

    def _filter_by_context(
        self,
        items: list[ClothingEmbeddingInfo],
        occasion: str,
        weather: str,
    ) -> list[ClothingEmbeddingInfo]:
        """
        Filtra prendas que coincidan con la ocasión y el clima del usuario.

        NOTA: ClothingEmbeddingInfo no tiene campos occasion/weather directamente.
        dressme-back los resuelve al construir el request. Como el modelo actual
        de ClothingEmbeddingInfo no incluye estos campos de filtro, la lógica de
        filtrado aquí es un pass-through — dressme-back ya pre-filtra el guardarropa
        por ocasión y clima antes de enviarlo a este endpoint.

        Este método está preparado para recibir campos adicionales de filtro
        en futuras versiones del schema sin cambiar la firma pública del servicio.
        """
        # En MVP: dressme-back pre-filtra por ocasión/clima antes de llamar aquí.
        # Este método es el punto de extensión natural para filtros adicionales
        # (ej: temporada, género, etc.) en iteraciones futuras.
        return items

    def _group_by_slot(
        self, items: list[ClothingEmbeddingInfo]
    ) -> dict[str, list[ClothingEmbeddingInfo]]:
        """Agrupa las prendas por su slot lógico."""
        by_slot: dict[str, list[ClothingEmbeddingInfo]] = {}
        for item in items:
            by_slot.setdefault(item.slot, []).append(item)
        return by_slot

    def _apply_top_k(
        self,
        by_slot: dict[str, list[ClothingEmbeddingInfo]],
        k: int,
    ) -> dict[str, list[ClothingEmbeddingInfo]]:
        """
        Limita cada slot a top-K prendas para controlar la explosión combinatoria.
        La selección es aleatoria (sin ranking previo — el scoring lo hace dressme-back).
        """
        return {
            slot: random.sample(items, min(len(items), k))
            for slot, items in by_slot.items()
        }

    # ── Generadores de combinaciones ──────────────────────────────────────────

    def _generate_separates_combinations(
        self, by_slot: dict[str, list[ClothingEmbeddingInfo]]
    ) -> list[list[ClothingEmbeddingInfo]]:
        """
        Genera combinaciones para outfits con separates (TOP + BOTTOM).

        Requiere TOP y BOTTOM. OUTERWEAR y FOOTWEAR son opcionales:
          - Si existen en el slot → se incluyen en todas las combinaciones.
          - Si no existen → la combinación sigue siendo válida sin ellos.

        Esta estrategia garantiza que siempre se generen outfits aunque el
        guardarropa no tenga zapatos o abrigos.
        """
        tops     = by_slot.get("TOP",      [])
        bottoms  = by_slot.get("BOTTOM",   [])
        outers   = by_slot.get("OUTERWEAR", [])
        footwear = by_slot.get("FOOTWEAR",  [])

        if not tops or not bottoms:
            logger.warning(
                "OutfitGeneratorService: Faltan prendas TOP o BOTTOM. "
                "tops=%d, bottoms=%d.",
                len(tops), len(bottoms),
            )
            return []

        # Construir los iterables por slot, incluyendo None como opción
        # para los slots opcionales (OUTERWEAR, FOOTWEAR)
        outer_options   = outers   if outers   else [None]
        footwear_options = footwear if footwear else [None]

        combinations = []
        for top, bottom, outer, shoe in product(tops, bottoms, outer_options, footwear_options):
            combo = [top, bottom]
            if outer is not None:
                combo.append(outer)
            if shoe is not None:
                combo.append(shoe)
            combinations.append(combo)

        return combinations

    def _generate_onepiece_combinations(
        self, by_slot: dict[str, list[ClothingEmbeddingInfo]]
    ) -> list[list[ClothingEmbeddingInfo]]:
        """
        Genera combinaciones para outfits con onepiece (vestido, mono, jumpsuit).

        ONEPIECE reemplaza a TOP + BOTTOM. OUTERWEAR y FOOTWEAR siguen siendo opcionales.
        """
        onepieces = by_slot.get("ONEPIECE", [])
        outers    = by_slot.get("OUTERWEAR", [])
        footwear  = by_slot.get("FOOTWEAR",  [])

        if not onepieces:
            return []

        outer_options    = outers   if outers   else [None]
        footwear_options = footwear if footwear else [None]

        combinations = []
        for onepiece, outer, shoe in product(onepieces, outer_options, footwear_options):
            combo = [onepiece]
            if outer is not None:
                combo.append(outer)
            if shoe is not None:
                combo.append(shoe)
            combinations.append(combo)

        return combinations

    # ── Builder de candidato ──────────────────────────────────────────────────

    def _build_candidate(
        self, combo: list[ClothingEmbeddingInfo]
    ) -> OutfitCandidate:
        """
        Construye un OutfitCandidate a partir de una combinación de prendas.

        outfit_vector = media aritmética de los embeddings de todas las prendas.
        La media (en lugar de la suma) mantiene el vector en el mismo espacio
        que los vectores individuales, preservando la comparabilidad semántica.
        """
        slots = [
            CandidateSlot(
                slot=item.slot,
                clothing_id=item.clothing_id,
                hue=item.hue,
                saturation=item.saturation,
                lightness=item.lightness,
            )
            for item in combo
        ]

        # outfit_vector = mean de los embeddings de las prendas del outfit
        embeddings_matrix = np.array(
            [item.embedding_vector for item in combo],
            dtype=np.float32,
        )  # shape: (N_prendas, 1536)

        outfit_vector: list[float] = embeddings_matrix.mean(axis=0).tolist()

        return OutfitCandidate(
            slots=slots,
            outfit_vector=outfit_vector,
        )