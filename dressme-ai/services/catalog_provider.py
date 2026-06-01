"""
services/catalog_provider.py
─────────────────────────────
Protocolo abstracto para proveedores de catálogo.

Solo esta interfaz es visible para los servicios.
La implementación concreta (CatalogClient) es privada de dependencies.py.
"""

from typing import Protocol

from schemas.wardrobe import CatalogData


class CatalogProvider(Protocol):
    """
    Protocolo que define el contrato para cualquier proveedor de catálogo.
    
    Permite que WardrobeAnalysisService NO conozca la implementación concreta.
    dependencies.py inyecta la implementación (CatalogClient u otra).
    """

    def get_catalog(self) -> CatalogData:
        """Devuelve el catálogo actual, aplicando caché si aplica."""
        ...
