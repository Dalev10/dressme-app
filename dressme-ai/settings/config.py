"""
settings/config.py
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Google Gemini ─────────────────────────────────────────────────────────
    gemini_api_key: str = ""

    # ── PostgreSQL ────────────────────────────────────────────────────────────
    db_host: str = "dressme-db"
    db_port: int = 5432
    db_name: str = "dressme"
    db_user: str
    db_pass: str

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