"""
dependencies.py
───────────────
Inyección de dependencias para FastAPI.

Define cómo se construyen los objetos que los routers necesitan.

FastAPI usa el patrón Depends() para inyección. Cuando un endpoint
declara `service: TasteVectorService = Depends(get_taste_vector_service)`,
FastAPI llama a get_taste_vector_service() y pasa el resultado al endpoint.

El EmbeddingService se construye una sola vez al arrancar
(patrón singleton implícito con lru_cache) para no reconfigurar
el cliente de Gemini en cada request.
"""

from functools import lru_cache
from settings.config import get_settings
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService


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
    lru_cache garantiza que genai.configure() solo se llama una vez
    durante la vida del proceso, no en cada request.
    """
    settings = get_settings()
    return EmbeddingService(api_key=settings.gemini_api_key)