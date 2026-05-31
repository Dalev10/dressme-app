"""
routers
────────
Módulo que agrupa todos los routers de la aplicación FastAPI.
"""

from routers.onboarding import router as onboarding
from routers.wardrobe   import router as wardrobe

__all__ = ["onboarding", "wardrobe"]