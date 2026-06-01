"""
scripts/seed_embeddings.py
"""

import os
import sys
import logging
import requests
import psycopg2
import time
from psycopg2.extras import RealDictCursor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)

DB_CONFIG = {
    "host":     os.environ["DB_HOST"],
    "port":     int(os.environ.get("DB_PORT", 5432)),
    "dbname":   os.environ["DB_NAME"],
    "user":     os.environ["DB_USER"],
    "password": os.environ["DB_PASS"],
}

AI_SERVICE_URL = os.environ.get("AI_SERVICE_URL", "http://127.0.0.1:8000")
GENERATE_ENDPOINT = f"{AI_SERVICE_URL.rstrip('/')}/internal/ai/onboarding/generate-embeddings"


def fetch_cards_without_embeddings(conn) -> list[dict]:
    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute("""
            SELECT id::text, semantic_description
            FROM tbl_style_cards
            WHERE embedding_vector IS NULL
              AND is_active = true
            ORDER BY display_order ASC
        """)
        rows = cur.fetchall()
    return [dict(row) for row in rows]


def call_generate_embeddings(cards: list[dict]) -> list[dict]:
    payload = {
        "style_cards": [
            {"id": card["id"], "semantic_description": card["semantic_description"]}
            for card in cards
        ]
    }

    logger.info("Llamando a %s con %d tarjetas...", GENERATE_ENDPOINT, len(cards))

    for intento in range(5):
        try:
            response = requests.post(GENERATE_ENDPOINT, json=payload, timeout=60)
            if response.status_code == 200:
                data = response.json()
                logger.info(
                    "Embeddings generados con modelo '%s' para %d tarjetas.",
                    data["model_used"], data["total_processed"],
                )
                return data["embeddings"]
            else:
                logger.error("El servicio AI respondió con %d: %s", response.status_code, response.text)
                sys.exit(1)
        except requests.exceptions.ConnectionError:
            if intento < 4:
                logger.warning("El servidor FastAPI aún está arrancando... Reintentando en 3 segundos...")
                time.sleep(3)
            else:
                logger.error("No se pudo conectar con FastAPI.")
                sys.exit(1)


def persist_embeddings(conn, embeddings: list[dict]) -> None:
    with conn.cursor() as cur:
        for item in embeddings:
            vector_str = "[" + ",".join(str(f) for f in item["embedding_vector"]) + "]"
            cur.execute(
                "UPDATE tbl_style_cards SET embedding_vector = %s::vector WHERE id = %s::uuid",
                (vector_str, item["id"]),
            )
            logger.info("  ✓ Embedding persistido para tarjeta %s", item["id"])
    conn.commit()
    logger.info("Commit realizado. %d embeddings guardados en DB.", len(embeddings))


def main():
    logger.info("═══════════════════════════════════════════════")
    logger.info("  seed_embeddings.py — Iniciando")
    logger.info("═══════════════════════════════════════════════")

    logger.info("Conectando a PostgreSQL en %s:%s/%s...", DB_CONFIG["host"], DB_CONFIG["port"], DB_CONFIG["dbname"])
    try:
        conn = psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        logger.error("No se pudo conectar a la base de datos: %s", e)
        sys.exit(1)

    try:
        cards = fetch_cards_without_embeddings(conn)

        if not cards:
            logger.info("Todas las style cards ya tienen embedding. Nada que hacer.")
            return

        logger.info("Encontradas %d tarjetas sin embedding.", len(cards))
        embeddings = call_generate_embeddings(cards)
        persist_embeddings(conn, embeddings)

        logger.info("═══════════════════════════════════════════════")
        logger.info("  seed_embeddings.py — Completado exitosamente")
        logger.info("═══════════════════════════════════════════════")

    except Exception as e:
        conn.rollback()
        logger.exception("Error inesperado durante el seed de embeddings: %s", e)
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
