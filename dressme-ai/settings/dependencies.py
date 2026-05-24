"""
dependencies.py
───────────────
Inyección de dependencias para FastAPI.
define cómo se construyen los objetos que los routers necesitan.

FastAPI usa el patrón Depends() para inyección. Cuando un endpoint
declara `service: TasteVectorService = Depends(get_taste_vector_service)`,
FastAPI llama a get_taste_vector_service() y pasa el resultado al endpoint.

La instancia del cliente OpenAI se crea una sola vez al arrancar
(patrón singleton implícito con lru_cache) para no abrir una nueva
conexión HTTP en cada request.
"""

from functools import lru_cache
from openai import OpenAI
from settings.config import get_settings
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService


@lru_cache(maxsize=1)
def get_openai_client() -> OpenAI:
    """
    Crea el cliente de OpenAI una sola vez durante la vida del proceso.
    lru_cache(maxsize=1) garantiza que siempre se devuelve la misma instancia.

    El cliente lee OPENAI_API_KEY automáticamente desde la variable de entorno,
    no hay que pasársela explícitamente. Si la variable no existe, OpenAI
    lanzará AuthenticationError en la primera llamada a la API.
    """
    settings = get_settings()
    return OpenAI(api_key=settings.openai_api_key)


def get_taste_vector_service() -> TasteVectorService:
    """
    TasteVectorService no depende de OpenAI (solo usa numpy),
    así que no necesita el cliente. Se puede instanciar limpiamente.
    """
    return TasteVectorService()


def get_embedding_service() -> EmbeddingService:
    """
    EmbeddingService sí necesita el cliente de OpenAI.
    Se construye pasándole el singleton del cliente.
    """
    client = get_openai_client()
    return EmbeddingService(openai_client=client)