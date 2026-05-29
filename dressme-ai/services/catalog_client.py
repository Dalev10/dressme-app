"""
services/catalog_client.py
───────────────────────────
Cliente HTTP que obtiene el catálogo activo de dressme-database.

Importaciones:
  - CatalogData viene de schemas.wardrobe (capa de datos compartidos).
  - Solo dependencies.py sabe que este servicio existe.
"""

import logging
import httpx

from schemas.wardrobe import CatalogData

logger = logging.getLogger(__name__)


class CatalogClient:
    """
    Obtiene y cachea el catálogo desde dressme-database.
    Instanciado como singleton en dependencies.py via lru_cache.
    """

    def __init__(self, database_url: str):
        self._database_url = database_url.rstrip("/")
        self._cache: CatalogData | None = None

    def get_catalog(self) -> CatalogData:
        """Devuelve el catálogo, refrescándolo si el TTL (10 min) expiró."""
        if self._cache is None or self._cache.is_expired():
            logger.info("CatalogClient: Refrescando catálogo desde dressme-database")
            self._cache = self._fetch_catalog()
            logger.info(
                "CatalogClient: Catálogo cargado — "
                "%d categorías, %d estilos, %d colores, %d climas, %d ocasiones",
                len(self._cache.categories), len(self._cache.styles),
                len(self._cache.colors),     len(self._cache.weathers),
                len(self._cache.occasions),
            )
        return self._cache

    def _fetch_catalog(self) -> CatalogData:
        url = f"{self._database_url}/internal/wardrobe/catalog"
        try:
            with httpx.Client(timeout=10.0) as client:
                response = client.get(url)
                response.raise_for_status()
        except httpx.HTTPStatusError as e:
            raise RuntimeError(
                f"dressme-database rechazó la solicitud de catálogo "
                f"({e.response.status_code}): {e.response.text}"
            ) from e
        except httpx.RequestError as e:
            raise RuntimeError(
                f"No se pudo conectar a dressme-database para el catálogo: {e}"
            ) from e

        data = response.json()
        return CatalogData(
            categories=[{"id": c["id"], "name": c["name"]} for c in data.get("categories", [])],
            styles    =[{"id": s["id"], "name": s["name"]} for s in data.get("styles",     [])],
            colors    =[{"id": c["id"], "name": c["name"]} for c in data.get("colors",     [])],
            weathers  =[{"id": w["id"], "name": w["name"]} for w in data.get("weathers",   [])],
            occasions =[{"id": o["id"], "name": o["name"]} for o in data.get("occasions",  [])],
        )

    def invalidate_cache(self) -> None:
        """Invalida el caché manualmente. Útil para tests."""
        self._cache = None
        logger.info("CatalogClient: Caché invalidado")