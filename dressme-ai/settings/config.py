"""
settings/config.py
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Google Gemini ─────────────────────────────────────────────────────────
    gemini_api_key: str = ""

    # ── Servicio ──────────────────────────────────────────────────────────────
    app_name: str = "dressme-ai"
    app_env: str = "development"
    log_level: str = "INFO"

    model_config = SettingsConfigDict(
        env_file=None,
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()