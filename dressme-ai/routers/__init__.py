"""
routers
────────
Módulo que agrupa todos los routers de la aplicación FastAPI.
"""

from routers.onboarding import router as onboarding_router

__all__ = ["onboarding_router"]
