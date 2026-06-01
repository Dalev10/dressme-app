"""
settings/dependencies.py  — VERSIÓN ACTUALIZADA
─────────────────────────────────────────────────
"""

from functools import lru_cache

from settings.config import get_settings
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService
from services.trend_score_service import TrendScoreService


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
    TrendScoreService sin dependencias de BD local.
    Consume dressme-database vía HTTP.
    lru_cache garantiza que el servicio se instancia una sola vez.
    """
    return TrendScoreService()