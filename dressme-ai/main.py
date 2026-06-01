import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from routers.trend import router as trend_router
from settings.config import get_settings
from infra.error_handler import register_error_handlers
from routers import onboarding, wardrobe
from routers.onboarding import router as onboarding_router


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

    if settings.gemini_api_key:
        logger.info("  Gemini API Key (embeddings + Vision) : OK")
    else:
        logger.warning(
            "  Gemini API Key NO configurada — "
            "los endpoints de embeddings y análisis de prendas fallarán. "
            "Agrega GEMINI_API_KEY al .env."
        )

    logger.info("  Database service URL                 : %s", settings.database_service_url)
    logger.info("══════════════════════════════════════════")

    yield

    logger.info("%s apagándose correctamente.", settings.app_name)


# ── Aplicación FastAPI ────────────────────────────────────────────────────────

app = FastAPI(
    title="Dressme AI Service",
    description="""
Motor de inteligencia artificial de Dressme.

**Responsabilidades en el MVP:**
- **Onboarding:** generar embeddings de style cards y calcular el taste vector del usuario.
- **Wardrobe:** analizar prendas con Gemini Vision, mapear al catálogo y extraer HSL del color dominante.

Consumido exclusivamente por **dressme-back** en la red interna Docker.
No está expuesto públicamente a través del Gateway.
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

register_error_handlers(app)

app.include_router(onboarding)
app.include_router(wardrobe)
app.include_router(trend_router)

logger.info("Routers registrados: onboarding, trend")

logger.info("Routers registrados: /internal/ai/onboarding/generate-embeddings, /internal/ai/onboarding/compute-taste-vector")

logger.info(
    "Routers registrados: "
    "/ai/onboarding/generate-embeddings, "
    "/ai/onboarding/compute-taste-vector, "
    "/ai/wardrobe/analyze"
)


@app.get("/", tags=["Health"])
def health_check():
    return {
        "service": settings.app_name,
        "status":  "active",
        "env":     settings.app_env,
    }
        "status": "active",
        "env": settings.app_env,
    }
