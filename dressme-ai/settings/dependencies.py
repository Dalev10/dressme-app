"""
settings/dependencies.py  — VERSIÓN ACTUALIZADA
─────────────────────────────────────────────────
"""

from functools import lru_cache

import psycopg2
import psycopg2.pool

from settings.config import get_settings
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService
from services.trend_score_service import TrendScoreService


# ── Pool de conexiones a PostgreSQL ───────────────────────────────────────────

@lru_cache(maxsize=1)
def get_db_pool() -> psycopg2.pool.ThreadedConnectionPool:
    """
    Crea un pool de conexiones a PostgreSQL la primera vez que se llama.
    lru_cache garantiza que solo existe un pool durante toda la vida del proceso.

    El pool se usa por TrendScoreService para consultar tbl_trend_dataset_config.
    minconn=1, maxconn=5 es suficiente para el volumen esperado del MVP.
    """
    settings = get_settings()
    return psycopg2.pool.ThreadedConnectionPool(
        minconn=1,
        maxconn=5,
        host=settings.db_host,
        port=settings.db_port,
        dbname=settings.db_name,
        user=settings.db_user,
        password=settings.db_pass,
    )


# ── Servicios ─────────────────────────────────────────────────────────────────

def get_taste_vector_service() -> TasteVectorService:
    """
    TasteVectorService no depende de ninguna API externa (solo usa numpy),
    así que no necesita credenciales. Se puede instanciar limpiamente.
    """
    return TasteVectorService()


@lru_cache(maxsize=1)
def get_embedding_service() -> EmbeddingService:
    """
    EmbeddingService necesita la Gemini API key.
    lru_cache garantiza que genai.configure() solo se llama una vez.
    """
    settings = get_settings()
    return EmbeddingService(api_key=settings.gemini_api_key)


@lru_cache(maxsize=1)
def get_trend_score_service() -> TrendScoreService:
    """
    TrendScoreService necesita el pool de DB para leer tbl_trend_dataset_config.
    lru_cache garantiza que el servicio se instancia una sola vez.
    """
    return TrendScoreService(db_pool=get_db_pool())