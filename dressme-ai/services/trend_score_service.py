"""
services/trend_score_service.py
────────────────────────────────
Calcula el trend score de un outfit comparando su vector
contra el vector promedio del dataset de moda actual.

Responsabilidades:
  1. Recibir los embeddings de las prendas del outfit.
  2. Calcular el outfit_vector como promedio de esos embeddings.
  3. Obtener el avg_vector del dataset desde PostgreSQL
     (tabla tbl_trend_dataset_config, fila más reciente).
  4. Calcular la similitud coseno entre ambos vectores.
  5. Normalizar el resultado a [0, 1] y devolverlo.

El acceso a DB se hace con psycopg2 mediante un pool de conexiones
que se inyecta en el constructor, igual que el resto del patrón del proyecto.
"""

import logging
import numpy as np
import psycopg2
import psycopg2.pool
from schemas.trend import TrendScoreRequest, TrendScoreResponse

logger = logging.getLogger(__name__)

# Dimensión esperada para cada embedding de prenda
EXPECTED_DIMS = 1536


class TrendScoreService:

    def __init__(self, db_pool: psycopg2.pool.ThreadedConnectionPool):
        """
        db_pool → pool de conexiones a PostgreSQL.
        Se inyecta desde dependencies.py al arrancar la aplicación.
        """
        self._db_pool = db_pool

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

    # ─── Helpers privados ─────────────────────────────────────────────────────

    def _load_latest_dataset_vector(self) -> tuple[np.ndarray, int] | None:
        """
        Carga el avg_vector más reciente de tbl_trend_dataset_config.

        Devuelve (avg_vector: ndarray, image_count: int) o None si la tabla
        está vacía (dataset aún no procesado).
        """
        conn = self._db_pool.getconn()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT avg_vector::text, image_count
                      FROM tbl_trend_dataset_config
                     ORDER BY computed_at DESC
                     LIMIT 1
                    """
                )
                row = cur.fetchone()

            if row is None:
                return None

            # pgvector devuelve el vector como string '[f1,f2,...]'
            # Lo parseamos a ndarray float32.
            vector_str, image_count = row
            values = [float(x) for x in vector_str.strip("[]").split(",")]
            avg_vector = np.array(values, dtype=np.float32)

            return avg_vector, image_count

        finally:
            self._db_pool.putconn(conn)

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