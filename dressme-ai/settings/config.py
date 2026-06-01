"""
settings/config.py
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Google Gemini (embeddings + Gemini Vision para prendas) ──────────────
    # Una sola API key cubre:
    #   - gemini-embedding-001  (onboarding: style card embeddings)
    #   - gemini-2.5-flash      (wardrobe: análisis visual de prendas)
    gemini_api_key: str = ""
 
    # ── dressme-database (catálogo de referencia para el mapeo) ───────────────
    # CatalogClient llama a GET /internal/wardrobe/catalog antes de cada
    # análisis (con caché en memoria de 10 min).
    database_service_url: str = "http://dressme-database:8080"
 
    # ── Servicio ──────────────────────────────────────────────────────────────
    app_name:  str = "dressme-ai"
    app_env:   str = "development"
    log_level: str = "INFO"
 
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
 
