"""
config.py
──────────
Gestión de configuración con pydantic-settings.

Pydantic Settings lee las variables en este orden de prioridad:
  1. Variables de entorno del sistema (las del docker-compose o .env)
  2. Archivo .env en el directorio raíz del proyecto
  3. Valores por defecto definidos en la clase

Si una variable marcada como requerida (sin default) no existe
en ninguna fuente, el servidor lanza ValidationError al arrancar,
con un mensaje claro de qué variable falta. Falla rápido y con claridad,
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict
 
 
class Settings(BaseSettings):
    # ── Google Gemini ─────────────────────────────────────────────────────────
    # Default vacío: el servicio arranca sin la key. Solo es requerida al
    # correr seed_embeddings.py o al analizar prendas (Fase 5+).
    # El lifespan de main.py emite un WARNING si no está configurada.
    gemini_api_key: str = ""
 
    # ── Servicio ──────────────────────────────────────────────────────────────
    app_name: str = "dressme-ai"
    app_env: str = "development"     # "development" | "production"
    log_level: str = "INFO"
 
    # ── Pydantic Settings config ───────────────────────────────────────────────
    model_config = SettingsConfigDict(
        env_file=".env",             # Lee el .env si existe (útil en local)
        env_file_encoding="utf-8",
        case_sensitive=False,        # GEMINI_API_KEY == gemini_api_key
        extra="ignore",              # Variables de entorno extra no causan error
    )
 
 
@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """
    Singleton de configuración.
    lru_cache garantiza que el .env solo se lee una vez al arrancar.
    """
    return Settings()