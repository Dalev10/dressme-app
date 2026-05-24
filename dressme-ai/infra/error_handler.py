import logging
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from openai import APIError, RateLimitError, APIConnectionError

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
    Ej: embedding con dimensiones distintas a 1536.
    → 422 Unprocessable Entity
    """
    logger.error("ValueError en %s: %s", request.url.path, exc)
    return _error_response(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        error="VALIDATION_ERROR",
        detail=str(exc),
    )


async def handle_rate_limit_error(request: Request, exc: RateLimitError) -> JSONResponse:
    """
    La API de OpenAI rechazó la solicitud por exceso de tasa.
    → 429 Too Many Requests
    """
    logger.error("RateLimitError de OpenAI en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_429_TOO_MANY_REQUESTS,
        error="OPENAI_RATE_LIMIT",
        detail="Límite de tasa de OpenAI alcanzado. Reintenta en unos segundos.",
    )


async def handle_api_connection_error(request: Request, exc: APIConnectionError) -> JSONResponse:
    """
    No se pudo establecer conexión con la API de OpenAI.
    Puede ser un problema de red o que OpenAI esté caído.
    → 503 Service Unavailable
    """
    logger.error("APIConnectionError de OpenAI en %s", request.url.path)
    return _error_response(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        error="OPENAI_UNAVAILABLE",
        detail="No se pudo conectar con OpenAI. Verifica la conectividad.",
    )


async def handle_openai_api_error(request: Request, exc: APIError) -> JSONResponse:
    """
    Error genérico de la API de OpenAI (autenticación, modelo no encontrado, etc.).
    RateLimitError y APIConnectionError son subclases de APIError, pero sus handlers
    están registrados primero, así que este solo captura los casos restantes.
    → 502 Bad Gateway
    """
    logger.error("APIError de OpenAI en %s: %s", request.url.path, exc.message)
    return _error_response(
        status_code=status.HTTP_502_BAD_GATEWAY,
        error="OPENAI_API_ERROR",
        detail=f"Error en la API de OpenAI: {exc.message}",
    )


async def handle_unhandled_exception(request: Request, exc: Exception) -> JSONResponse:
    """
    Última línea de defensa: cualquier excepción no capturada por los handlers
    anteriores llega aquí. Equivalente al catch(Exception e) genérico.
    → 500 Internal Server Error

    Se usa logger.exception() para incluir el stack trace completo en los logs,
    imprescindible para debuggear errores inesperados en producción.
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
    a más general. RateLimitError y APIConnectionError deben registrarse
    ANTES que APIError (su clase padre), o nunca se alcanzarían.
    """
    app.add_exception_handler(RateLimitError, handle_rate_limit_error)
    app.add_exception_handler(APIConnectionError, handle_api_connection_error)
    app.add_exception_handler(APIError, handle_openai_api_error)
    app.add_exception_handler(ValueError, handle_value_error)
    app.add_exception_handler(Exception, handle_unhandled_exception)