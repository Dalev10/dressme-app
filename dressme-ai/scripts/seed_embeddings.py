"""
scripts/seed_embeddings.py
───────────────────────────
Script one-shot que:
  1. Lee las 12 style cards de tbl_style_cards (las que tienen embedding_vector = NULL)
  2. Llama al endpoint POST /ai/onboarding/generate-embeddings
  3. Persiste los vectores resultantes en tbl_style_cards.embedding_vector

Se ejecuta UNA SOLA VEZ después del primer arranque del sistema,
o cada vez que se agreguen style cards nuevas.

Uso:
    # Desde la raíz de dressme-ai, con el servicio corriendo:
    python scripts/seed_embeddings.py

Variables de entorno requeridas (mismas que el servicio):
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
    AI_SERVICE_URL  →  URL interna de dressme-ai (default: http://localhost:8000)
"""

import os
import sys
import logging
import requests
import psycopg2
from psycopg2.extras import RealDictCursor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)

# ── Configuración ─────────────────────────────────────────────────────────────

DB_CONFIG = {
    "host":     os.environ["DB_HOST"],
    "port":     int(os.environ.get("DB_PORT", 5432)),
    "dbname":   os.environ["DB_NAME"],
    "user":     os.environ["DB_USER"],
    "password": os.environ["DB_PASS"],
}

AI_SERVICE_URL = os.environ.get("AI_SERVICE_URL", "http://localhost:8000")
GENERATE_ENDPOINT = f"{AI_SERVICE_URL}/ai/onboarding/generate-embeddings"


# ── Paso 1: leer tarjetas sin embedding ───────────────────────────────────────

def fetch_cards_without_embeddings(conn) -> list[dict]:
    """
    Consulta las style cards que aún no tienen embedding generado.
    Idempotente: si todas ya tienen embedding, devuelve lista vacía
    y el script termina sin hacer ninguna llamada a OpenAI.
    """
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


# ── Paso 2: llamar al servicio AI ─────────────────────────────────────────────

def call_generate_embeddings(cards: list[dict]) -> list[dict]:
    """
    Llama a POST /ai/onboarding/generate-embeddings con todas las tarjetas.
    Devuelve la lista de {id, embedding_vector} lista para persistir.
    """
    payload = {
        "style_cards": [
            {
                "id": card["id"],
                "semantic_description": card["semantic_description"],
            }
            for card in cards
        ]
    }

    logger.info("Llamando a %s con %d tarjetas...", GENERATE_ENDPOINT, len(cards))

    response = requests.post(
        GENERATE_ENDPOINT,
        json=payload,
        timeout=60,  # OpenAI puede tardar hasta ~10s para batch de 12
    )

    if response.status_code != 200:
        logger.error(
            "El servicio AI respondió con %d: %s",
            response.status_code,
            response.text,
        )
        sys.exit(1)

    data = response.json()
    logger.info(
        "Embeddings generados con modelo '%s' para %d tarjetas.",
        data["model_used"],
        data["total_processed"],
    )
    return data["embeddings"]


# ── Paso 3: persistir en DB ───────────────────────────────────────────────────

def persist_embeddings(conn, embeddings: list[dict]) -> None:
    """
    Actualiza tbl_style_cards con los vectores generados.

    psycopg2 no tiene soporte nativo para el tipo vector de pgvector,
    así que serializamos el array como string '[f1, f2, ...]' que
    PostgreSQL acepta y convierte automáticamente al tipo vector.
    """
    with conn.cursor() as cur:
        for item in embeddings:
            # Serializar el array de floats al formato que acepta pgvector
            vector_str = "[" + ",".join(str(f) for f in item["embedding_vector"]) + "]"

            cur.execute(
                """
                UPDATE tbl_style_cards
                SET embedding_vector = %s::vector
                WHERE id = %s::uuid
                """,
                (vector_str, item["id"]),
            )
            logger.info("  ✓ Embedding persistido para tarjeta %s", item["id"])

    conn.commit()
    logger.info("Commit realizado. %d embeddings guardados en DB.", len(embeddings))


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    logger.info("═══════════════════════════════════════════════")
    logger.info("  seed_embeddings.py — Iniciando")
    logger.info("═══════════════════════════════════════════════")

    # Conectar a PostgreSQL
    logger.info("Conectando a PostgreSQL en %s:%s/%s...",
                DB_CONFIG["host"], DB_CONFIG["port"], DB_CONFIG["dbname"])
    try:
        conn = psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        logger.error("No se pudo conectar a la base de datos: %s", e)
        sys.exit(1)

    try:
        # Paso 1: ¿hay tarjetas sin embedding?
        cards = fetch_cards_without_embeddings(conn)

        if not cards:
            logger.info("Todas las style cards ya tienen embedding. Nada que hacer.")
            return

        logger.info("Encontradas %d tarjetas sin embedding.", len(cards))

        # Paso 2: generar embeddings vía servicio AI
        embeddings = call_generate_embeddings(cards)

        # Paso 3: persistir en DB
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