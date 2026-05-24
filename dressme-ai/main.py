"""
main.py
────────
Punto de entrada de dressme-ai.

Equivalente al DressmeAiApplication.java de Spring Boot.
"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI

from settings.config import get_settings
from infra.error_handler import register_error_handlers
from routers import onboarding_router


# ── Configuración de logging ──────────────────────────────────────────────────

def configure_logging(log_level: str) -> None:
    logging.basicConfig(
        level=getattr(logging, log_level.upper(), logging.INFO),
        format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

settings = get_settings()
configure_logging(settings.log_level)
logger = logging.getLogger(__name__)


# ── Lifespan ──────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("══════════════════════════════════════════")
    logger.info("  %s arrancando en modo: %s", settings.app_name, settings.app_env)
    if settings.openai_api_key:
        logger.info("  OpenAI API Key configurada: OK")
    else:
        logger.warning(
            "  OpenAI API Key NO configurada — "
            "los endpoints de embeddings fallarán si se invocan. "
            "Agrega OPENAI_API_KEY al .env cuando estés listo."
        )
    logger.info("══════════════════════════════════════════")
    yield
    logger.info("%s apagándose correctamente.", settings.app_name)


# ── Aplicación FastAPI ────────────────────────────────────────────────────────

app = FastAPI(
    title="Dressme AI Service",
    description="""
    Motor de inteligencia artificial de Dressme.

    Responsabilidades en el MVP:
    - **Generar embeddings** de style cards usando OpenAI text-embedding-3-small
    - **Calcular el taste vector** del usuario a partir de sus selecciones de onboarding

    Consumido exclusivamente por **dressme-back** en la red interna Docker.
    No está expuesto públicamente a través del Gateway.
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# ── Registro de error handlers ────────────────────────────────────────────────
register_error_handlers(app)

# ── Registro de routers ───────────────────────────────────────────────────────
app.include_router(onboarding_router)


# ── Health check ─────────────────────────────────────────────────────────────

@app.get("/", tags=["Health"])
def health_check():
    return {
        "service": settings.app_name,
        "status": "active",
        "env": settings.app_env,
    }