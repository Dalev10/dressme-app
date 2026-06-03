"""
services/dresscode_similarity_service.py
─────────────────────────────────────────
Calcula la similitud coseno entre el outfit_vector de un candidato y el
dresscode_embedding del dress code seleccionado por el usuario.

Responsabilidades:
  - Recibir el outfit_vector (promedio de embeddings de las prendas del candidato)
    y el dresscode_embedding (vector del dress code, llegado en el request).
  - Calcular la similitud coseno entre ambos vectores.
  - Normalizar el resultado al rango [0.0, 1.0] (la similitud coseno puede ser
    negativa si los vectores apuntan en direcciones opuestas → se clampea a 0).
  - Devolver DresscodeSimilarityResponse(score, applies).

Contrato de applies:
  applies = True  → dresscode_embedding no es None; el score es significativo.
  applies = False → dresscode_embedding es None; el usuario no seleccionó dress code.
                    El ScoreEngine en dressme-back redistribuirá el peso del
                    componente dresscode entre los demás componentes, igual que
                    hace con color_score cuando applies=False.

Diseño:
  - Stateless: sin estado interno, sin dependencias externas, sin constructor
    con argumentos. Se puede instanciar directamente desde dependencies.py
    sin necesidad de lru_cache (no hay recursos costosos que reutilizar).
  - Numpy puro para el cálculo vectorial, coherente con el resto de los
    servicios de scoring del microservicio (TasteVectorService, TrendScoreService).
  - No valida la dimensionalidad de los vectores: esa responsabilidad recae en
    los schemas Pydantic (OutfitGenerationOrchestratedRequest, OutfitCandidate).
    Si llegan vectores de dimensiones distintas, numpy lanzará un ValueError
    que el error_handler global convierte en 422.
"""

import logging

import numpy as np
from pydantic import BaseModel

logger = logging.getLogger(__name__)


# ── Response ──────────────────────────────────────────────────────────────────

class DresscodeSimilarityResponse(BaseModel):
    """
    Resultado del cálculo de similitud coseno con el dress code.

    score   → similitud coseno clampeada a [0.0, 1.0].
               0.0 cuando applies=False (sin dress code).
    applies → True si se calculó un score real;
               False si el usuario no seleccionó dress code.
    """
    score:   float
    applies: bool


# ── Servicio ──────────────────────────────────────────────────────────────────

class DresscodeSimilarityService:
    """
    Calcula la similitud coseno entre outfit_vector y dresscode_embedding.

    Stateless — sin dependencias externas. Instanciado en dependencies.py
    sin lru_cache porque no hay estado costoso que amortizar.
    """

    def compute(
        self,
        outfit_vector:       list[float],
        dresscode_embedding: list[float] | None,
    ) -> DresscodeSimilarityResponse:
        """
        Calcula la similitud entre el vector del outfit y el del dress code.

        Parámetros
        ----------
        outfit_vector:
            Promedio de los embeddings de las prendas del candidato (1536 dims).
            Calculado por OutfitGeneratorService._build_candidate().
        dresscode_embedding:
            Vector del dress code seleccionado por el usuario (1536 dims).
            None si el usuario no especificó dress code.

        Retorno
        -------
        DresscodeSimilarityResponse con:
          - applies=False, score=0.0  si dresscode_embedding es None.
          - applies=True,  score∈[0,1] si dresscode_embedding tiene valor.

        Casos borde:
          - Vector cero (norma ≈ 0): se devuelve score=0.0 con applies=True
            para evitar división por cero. Un outfit sin semántica clara
            no tiene afinidad con ningún dress code.
        """
        if dresscode_embedding is None:
            logger.debug(
                "DresscodeSimilarityService: dresscode_embedding es None — "
                "applies=False, score=0.0. "
                "El ScoreEngine en dressme-back redistribuirá el peso."
            )
            return DresscodeSimilarityResponse(score=0.0, applies=False)

        score = self._cosine_similarity(outfit_vector, dresscode_embedding)

        logger.debug(
            "DresscodeSimilarityService: dresscode_score=%.4f (applies=True)",
            score,
        )
        return DresscodeSimilarityResponse(score=score, applies=True)

    # ── Cálculo interno ───────────────────────────────────────────────────────

    def _cosine_similarity(
        self,
        vec_a: list[float],
        vec_b: list[float],
    ) -> float:
        """
        Similitud coseno entre dos vectores, clampeada a [0.0, 1.0].

        La similitud coseno puede ser negativa cuando los vectores apuntan
        en direcciones opuestas. En el contexto del scoring de outfits,
        una similitud negativa se interpreta como "sin afinidad", equivalente
        a 0.0, para no penalizar al ScoreEngine con scores negativos.

        Parámetros
        ----------
        vec_a, vec_b:
            Vectores de igual dimensionalidad (1536 dims en producción).

        Retorno
        -------
        float en [0.0, 1.0].
        """
        a = np.array(vec_a, dtype=np.float32)
        b = np.array(vec_b, dtype=np.float32)

        norm_a = float(np.linalg.norm(a))
        norm_b = float(np.linalg.norm(b))

        if norm_a < 1e-9 or norm_b < 1e-9:
            logger.warning(
                "DresscodeSimilarityService: Uno de los vectores tiene norma ≈ 0 "
                "(outfit_vector norma=%.6f, dresscode norma=%.6f). "
                "Devolviendo score=0.0.",
                norm_a,
                norm_b,
            )
            return 0.0

        raw_similarity = float(np.dot(a, b) / (norm_a * norm_b))

        # Clamp a [0, 1]: similitudes negativas → sin afinidad con el dress code.
        return max(0.0, raw_similarity)