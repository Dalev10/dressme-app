"""
settings/dependencies.py
─────────────────────────
Único punto de ensamblado de la aplicación.

Principio fundamental:
  Solo dependencies.py conoce todas las implementaciones concretas
  y decide cómo conectarlas. Los servicios solo conocen protocolos.

Flujo de construcción al arrancar:
  get_catalog_client()              → instancia CatalogClient (implementación concreta)
  get_wardrobe_analysis_service()   → inyecta CatalogClient como CatalogProvider
                                      WardrobeAnalysisService recibe el protocolo,
                                      nunca la clase concreta.

Patrón singleton via lru_cache:
  Igual que get_embedding_service():
  los servicios con estado externo (conexiones, clientes de API) se crean
  una sola vez durante la vida del proceso.
"""

from functools import lru_cache

from settings.config import get_settings
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService
from services.trend_score_service import TrendScoreService
from services.catalog_client import CatalogClient
from services.wardrobe import WardrobeAnalysisService
from services.embedding_clothing_service import EmbeddingClothingService
from services.outfit_generator_service import OutfitGeneratorService


# ── Servicios ─────────────────────────────────────────────────────────────────

def get_taste_vector_service() -> TasteVectorService:
    return TasteVectorService()


@lru_cache(maxsize=1)
def get_embedding_service() -> EmbeddingService:
    settings = get_settings()
    return EmbeddingService(api_key=settings.gemini_api_key)


# ── Wardrobe ──────────────────────────────────────────────────────────────────

@lru_cache(maxsize=1)
def get_catalog_client() -> CatalogClient:
    """
    Instancia la implementación concreta del CatalogProvider.
    Solo dependencies.py sabe que existe CatalogClient.
    WardrobeAnalysisService lo recibe como CatalogProvider (protocolo).
    """
    settings = get_settings()
    return CatalogClient(database_url=settings.database_service_url)


@lru_cache(maxsize=1)
def get_wardrobe_analysis_service() -> WardrobeAnalysisService:
    """
    Ensambla WardrobeAnalysisService inyectando:
      - api_key: str             → desde settings (misma key que EmbeddingService)
      - catalog: CatalogProvider → la implementación CatalogClient

    Si en el futuro el catálogo viene de Redis, solo hay que cambiar
    get_catalog_client() para devolver RedisCatalogClient.
    WardrobeAnalysisService no se toca.
    """
    settings = get_settings()

    return WardrobeAnalysisService(
        api_key=settings.gemini_api_key,
        catalog=get_catalog_client(),   # inyectado como CatalogProvider
    )


# ── Outfit ────────────────────────────────────────────────────────────────────
 
@lru_cache(maxsize=1)
def get_embedding_clothing_service() -> EmbeddingClothingService:
    settings = get_settings()
    return EmbeddingClothingService(api_key=settings.gemini_api_key)
 
 
def get_outfit_generator_service() -> OutfitGeneratorService:
    return OutfitGeneratorService(
        catalog=get_catalog_client(),  # inyectado como CatalogProvider
    )


# ── Trend Score ───────────────────────────────────────────────────────────────

@lru_cache(maxsize=1)
def get_trend_score_service() -> TrendScoreService:
    """
    TrendScoreService sin dependencias de BD local.
    Consume dressme-database vía HTTP.

    lru_cache garantiza que el servicio se instancia una sola vez.
    """
    return TrendScoreService()
