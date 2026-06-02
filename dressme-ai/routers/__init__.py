
"""
routers
────────
Módulo que agrupa todos los routers de la aplicación FastAPI.
"""

from routers.onboarding import router as onboarding
from routers.wardrobe   import router as wardrobe
from routers.outfit     import router as outfit
from routers.trend import router as trend_router

__all__ = ["onboarding", "wardrobe", "trend_router", "outfit"]
