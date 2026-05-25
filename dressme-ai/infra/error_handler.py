import logging
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from google.api_core.exceptions import (
    GoogleAPIError,
    ResourceExhausted,
    Unauthenticated,
    ServiceUnavailable,
)

logger = logging.getLogger(__name__)


def _error_response(status_code: int, error: str, detail: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={"error": error, "detail": detail},
    )


# ── Handlers ──────────────────────────────────────────────────────────────────

async def handle_value_error(request: Request, exc: ValueError) -> JSONResponse:
    """
    Errores de validación de dominio.
    Ej: embedding con dimensiones distintas a 1536, embeddings nulos.
    → 422 Unprocessable Entity
    """
    logger.error("ValueError en %s: %s", request.url.path, exc)
    return _error_response(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        error="VALIDATION_ERROR",
        detail=str(exc),
    )


async def handle_resource_exhausted(request: Request, exc: ResourceExhausted) -> JSONResponse:
    """
    Límite de tasa de la API de Gemini alcanzado (equivalente al 429).
    En el free tier: 15 RPM para embeddings.
    → 429 Too Many Requests
    """
    logger.error("ResourceExhausted de Gemini en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_429_TOO_MANY_REQUESTS,
        error="GEMINI_RATE_LIMIT",
        detail="Límite de tasa de Gemini alcanzado. Reintenta en unos segundos.",
    )


async def handle_unauthenticated(request: Request, exc: Unauthenticated) -> JSONResponse:
    """
    API key inválida, expirada o no configurada.
    → 401 Unauthorized
    """
    logger.error("Unauthenticated de Gemini en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_401_UNAUTHORIZED,
        error="GEMINI_AUTH_ERROR",
        detail="API key de Gemini inválida o no configurada. Verifica GEMINI_API_KEY en el .env.",
    )


async def handle_service_unavailable(request: Request, exc: ServiceUnavailable) -> JSONResponse:
    """
    La API de Gemini no está disponible temporalmente.
    → 503 Service Unavailable
    """
    logger.error("ServiceUnavailable de Gemini en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        error="GEMINI_UNAVAILABLE",
        detail="La API de Gemini no está disponible temporalmente. Reintenta más tarde.",
    )


async def handle_google_api_error(request: Request, exc: GoogleAPIError) -> JSONResponse:
    """
    Error genérico de la API de Google.
    ResourceExhausted, Unauthenticated y ServiceUnavailable son subclases
    de GoogleAPIError pero sus handlers están registrados primero, así que
    este solo captura los casos restantes.
    → 502 Bad Gateway
    """
    logger.error("GoogleAPIError en %s: %s", request.url.path, str(exc))
    return _error_response(
        status_code=status.HTTP_502_BAD_GATEWAY,
        error="GEMINI_API_ERROR",
        detail=f"Error en la API de Gemini: {str(exc)}",
    )


async def handle_unhandled_exception(request: Request, exc: Exception) -> JSONResponse:
    """
    Última línea de defensa: cualquier excepción no capturada por los handlers
    anteriores llega aquí.
    → 500 Internal Server Error
    """
    logger.exception("Excepción no controlada en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        error="INTERNAL_ERROR",
        detail="Error interno del servidor.",
    )


# ── Registro ──────────────────────────────────────────────────────────────────

def register_error_handlers(app: FastAPI) -> None:
    """
    Registra todos los handlers en la aplicación FastAPI.

    El orden importa: FastAPI evalúa los handlers de más específico
    a más general. Las subclases deben registrarse ANTES que su clase padre
    GoogleAPIError, o nunca se alcanzarían.
    """
    app.add_exception_handler(ResourceExhausted, handle_resource_exhausted)
    app.add_exception_handler(Unauthenticated, handle_unauthenticated)
    app.add_exception_handler(ServiceUnavailable, handle_service_unavailable)
    app.add_exception_handler(GoogleAPIError, handle_google_api_error)
    app.add_exception_handler(ValueError, handle_value_error)
    app.add_exception_handler(Exception, handle_unhandled_exception)