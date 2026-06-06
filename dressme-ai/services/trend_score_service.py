"""
services/trend_score_service.py
────────────────────────────────
Calcula el trend score de un outfit comparando su vector
contra el vector promedio del dataset de moda actual.

Responsabilidades:
  1. Recibir los embeddings de las prendas del outfit.
  2. Calcular el outfit_vector como promedio de esos embeddings.
  3. Obtener el avg_vector del dataset desde dressme-database
     vía HTTP endpoint GET /internal/trend-dataset/config/latest.
  4. Calcular la similitud coseno entre ambos vectores.
  5. Normalizar el resultado a [0, 1] y devolverlo.

Acceso a datos centralizado en dressme-database siguiendo la arquitectura
de microservicios. Solo HTTP, sin conexiones directas a PostgreSQL.
"""

import logging
import os
import time
import numpy as np
import requests
from schemas.trend import TrendScoreRequest, TrendScoreResponse, TrendScoreBatchRequest, TrendScoreBatchResponse

logger = logging.getLogger(__name__)

EXPECTED_DIMS = 1536

DATABASE_SERVICE_URL = os.environ.get(
    "DATABASE_SERVICE_URL",
    "http://dressme-database:8080"
)
LATEST_DATASET_ENDPOINT = f"{DATABASE_SERVICE_URL}/internal/trend-dataset/config/latest"

# TTL en segundos para el caché del avg_vector (default 5 min)
AVG_VECTOR_CACHE_TTL = int(os.environ.get("TREND_VECTOR_CACHE_TTL", "300"))


class TrendScoreService:

    def __init__(self):
        self._cached_vector: tuple[np.ndarray, int] | None = None
        self._cached_at: float = 0.0

    # ─── API pública ───────────────────────────────────────────────────────────

    def score(self, request: TrendScoreRequest) -> TrendScoreResponse:
        """
        Calcula el trend score del outfit.

        Pasos:
          1. Validar dimensiones de cada embedding recibido.
          2. Calcular outfit_vector = promedio de los embeddings.
          3. Cargar avg_vector desde PostgreSQL.
          4. Calcular similitud coseno.
          5. Clamp a [0, 1] (la similitud coseno puede ser negativa si
             los vectores apuntan en direcciones opuestas).
        """
        logger.info(
            "TrendScoreService: Calculando trend score para outfit con %d prendas",
            len(request.outfit_embeddings),
        )

        # ── Paso 1: validar dimensiones ───────────────────────────────────────
        for i, emb in enumerate(request.outfit_embeddings):
            if len(emb) != EXPECTED_DIMS:
                raise ValueError(
                    f"El embedding en posición {i} tiene {len(emb)} dims, "
                    f"se esperaban {EXPECTED_DIMS}."
                )

        # ── Paso 2: outfit_vector = promedio de embeddings de prendas ─────────
        matrix = np.array(request.outfit_embeddings, dtype=np.float32)  # (N, 1536)
        outfit_vector = matrix.mean(axis=0)                              # (1536,)

        # ── Paso 3: cargar avg_vector desde DB ───────────────────────────────
        dataset_row = self._load_latest_dataset_vector()

        if dataset_row is None:
            logger.warning(
                "TrendScoreService: No hay vector de dataset en tbl_trend_dataset_config. "
                "Devolviendo applies=False para que el ScoreEngine redistribuya el peso."
            )
            return TrendScoreResponse(trend_score=0.0, applies=False, dataset_images=0)

        avg_vector, image_count = dataset_row

        # ── Paso 4: similitud coseno ──────────────────────────────────────────
        similarity = self._cosine_similarity(outfit_vector, avg_vector)

        # ── Paso 5: renormalización lineal a [0, 1] ────────────────────────────────────────────
        # La similitud coseno vive en [-1, 1]. Para el scoring debemos tener un valor
        # positivo logrando así que un outfit antitendecia baja su score y uno neutro queda en 0.5
        trend_score = float((similarity + 1.0) / 2.0)

        logger.info(
            "TrendScoreService: trend_score=%.4f (cosine_similarity=%.4f, dataset_images=%d)",
            trend_score,
            similarity,
            image_count,
        )

        return TrendScoreResponse(
            trend_score=trend_score,
            applies=True,
            dataset_images=image_count,
        )

    def score_batch(self, request: TrendScoreBatchRequest) -> TrendScoreBatchResponse:
        logger.info(
            "TrendScoreService: score_batch — %d outfits",
            len(request.outfits),
        )

        # Carga avg_vector una sola vez para todo el batch (caché incluido)
        dataset_row = self._load_latest_dataset_vector()

        results: list[TrendScoreResponse] = []
        for i, outfit_embeddings in enumerate(request.outfits):
            if not outfit_embeddings:
                results.append(TrendScoreResponse(trend_score=0.0, applies=False, dataset_images=0))
                continue

            for j, emb in enumerate(outfit_embeddings):
                if len(emb) != EXPECTED_DIMS:
                    raise ValueError(
                        f"Outfit {i}, embedding {j}: {len(emb)} dims, se esperaban {EXPECTED_DIMS}."
                    )

            if dataset_row is None:
                results.append(TrendScoreResponse(trend_score=0.0, applies=False, dataset_images=0))
                continue

            avg_vector, image_count = dataset_row
            matrix = np.array(outfit_embeddings, dtype=np.float32)
            outfit_vector = matrix.mean(axis=0)
            similarity = self._cosine_similarity(outfit_vector, avg_vector)
            trend_score = float((similarity + 1.0) / 2.0)

            results.append(TrendScoreResponse(
                trend_score=trend_score,
                applies=True,
                dataset_images=image_count,
            ))

        logger.info("TrendScoreService: score_batch completado — %d scores", len(results))
        return TrendScoreBatchResponse(scores=results)

    # ─── Helpers privados ─────────────────────────────────────────────────────

    def _load_latest_dataset_vector(self) -> tuple[np.ndarray, int] | None:
        now = time.monotonic()
        if self._cached_vector is not None and (now - self._cached_at) < AVG_VECTOR_CACHE_TTL:
            logger.debug("TrendScoreService: avg_vector servido desde caché")
            return self._cached_vector

        fresh = self._fetch_dataset_vector()
        if fresh is not None:
            self._cached_vector = fresh
            self._cached_at = now
        return fresh

    def _fetch_dataset_vector(self) -> tuple[np.ndarray, int] | None:
        try:
            response = requests.get(LATEST_DATASET_ENDPOINT, timeout=10)
        except requests.RequestException as e:
            logger.error(
                "TrendScoreService: Error conectando a dressme-database en %s: %s",
                LATEST_DATASET_ENDPOINT,
                e,
            )
            return None

        if response.status_code == 404:
            logger.warning("TrendScoreService: No hay vector de dataset en dressme-database")
            return None

        if response.status_code != 200:
            logger.error(
                "TrendScoreService: dressme-database respondió con status=%d: %s",
                response.status_code,
                response.text,
            )
            return None

        try:
            data = response.json()
            image_count = data.get("imageCount", 0)
            avg_vector = data.get("avgVector")
            if not avg_vector:
                logger.error("TrendScoreService: Respuesta de dressme-database no incluye avgVector")
                return None
            values = np.array(avg_vector, dtype=np.float32)
            logger.info(
                "TrendScoreService: avg_vector cargado desde dressme-database — imageCount=%d, TTL=%ds",
                image_count,
                AVG_VECTOR_CACHE_TTL,
            )
            return values, image_count
        except (KeyError, ValueError) as e:
            logger.error("TrendScoreService: Error parseando respuesta de dressme-database: %s", e)
            return None

    @staticmethod
    def _cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
        """
        Similitud coseno entre dos vectores.

        Si alguno es el vector cero, devuelve 0.0 para evitar división por cero.
        """
        norm_a = float(np.linalg.norm(a))
        norm_b = float(np.linalg.norm(b))

        if norm_a < 1e-9 or norm_b < 1e-9:
            logger.warning(
                "TrendScoreService: Uno de los vectores es cero. "
                "Similitud coseno indefinida → 0.0"
            )
            return 0.0

        return float(np.dot(a, b) / (norm_a * norm_b))