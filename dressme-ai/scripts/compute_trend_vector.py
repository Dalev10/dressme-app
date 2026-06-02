"""
scripts/compute_trend_vector.py
SDK: google-genai==2.7.0
"""

import argparse
import datetime
import json
import logging
import os
import re
import sys
import time
from pathlib import Path

import numpy as np
import requests
from google import genai
from google.genai import types
from jose import jwt
from PIL import Image
from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv())

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
CHECKPOINT_FILE      = Path("checkpoint_prendas.json")
METADATA_FILE        = Path("trend_metadata_report.json")

VISION_MODEL_NAME    = "gemini-2.5-flash"
EMBEDDING_MODEL_NAME = "gemini-embedding-001"
EXPECTED_DIMS        = 1536

BACK_URL               = os.environ.get("BACK_URL", "http://dressme-back:8080")
TREND_DATASET_ENDPOINT = f"{BACK_URL}/internal/trend-dataset/config"
INTERNAL_JWT_SECRET    = os.environ.get(
    "INTERNAL_JWT_SECRET",
    os.environ.get("JWT_SECRET", "dressme-secret-key-change-in-production-minimum-256-bits-required-12345")
)
INTERNAL_SERVICE_NAME = "dressme-ai-script"

_client: genai.Client | None = None

def get_client() -> genai.Client:
    global _client
    if _client is None:
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            logger.error("Falta GEMINI_API_KEY en las variables de entorno.")
            sys.exit(1)
        _client = genai.Client(api_key=api_key)
    return _client


def format_time(seconds: float) -> str:
    return str(datetime.timedelta(seconds=int(seconds)))


def load_checkpoint() -> dict:
    if CHECKPOINT_FILE.exists():
        with open(CHECKPOINT_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_checkpoint(data: dict):
    with open(CHECKPOINT_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def generate_metadata_report(success_images: int, total_garments: int, status: str):
    metadata = {
        "timestamp_generacion":             datetime.datetime.now().isoformat(),
        "estado_ejecucion":                 status,
        "imagenes_procesadas_exitosamente": success_images,
        "prendas_totales_extraidas":        total_garments,
        "dimensiones_vector":               EXPECTED_DIMS,
        "modelos_utilizados": {
            "vision":    VISION_MODEL_NAME,
            "embedding": EMBEDDING_MODEL_NAME,
        },
    }
    with open(METADATA_FILE, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=4, ensure_ascii=False)
    logger.info("\n[+] Reporte de metadatos generado en: %s", METADATA_FILE)


def discover_images(dataset_path: Path) -> list[Path]:
    if not dataset_path.exists():
        logger.error("El directorio '%s' no existe.", dataset_path)
        sys.exit(1)

    images = [
        p for p in dataset_path.rglob("*")
        if p.is_file() and p.suffix.lower() in SUPPORTED_EXTENSIONS
    ]

    if not images:
        logger.error("No se encontraron imágenes compatibles en '%s'.", dataset_path)
        sys.exit(1)

    return sorted(images)


def extract_garments_metadata(image_path: Path) -> list[str]:
    client = get_client()

    with open(image_path, "rb") as f:
        image_bytes = f.read()

    suffix = image_path.suffix.lower()
    mime_map = {".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".webp": "image/webp"}
    mime_type = mime_map.get(suffix, "image/jpeg")

    prompt = (
        "Analiza esta imagen de moda e identifica cada prenda de ropa individual. "
        "Para cada prenda, extrae exactamente estos 5 atributos: categoria, estilo, color, clima y ocasion. "
        "Devuelve la respuesta UNICAMENTE como un arreglo JSON valido, sin markdown, sin explicaciones. "
        "Ejemplo: [{\"categoria\": \"pantalones\", \"estilo\": \"urbano\", \"color\": \"azul\", \"clima\": \"templado\", \"ocasion\": \"casual\"}]"
    )

    response = client.models.generate_content(
        model=VISION_MODEL_NAME,
        contents=[
            types.Part.from_bytes(data=image_bytes, mime_type=mime_type),
            types.Part.from_text(text=prompt),
        ],
    )

    raw = response.text or ""
    cleaned = re.sub(r"```(?:json)?", "", raw).strip()
    if not cleaned:
        raise ValueError(f"Gemini devolvio respuesta vacia para {image_path.name}")

    items = json.loads(cleaned)
    descriptions = []
    for item in items:
        text_desc = (
            f"Categoria: {item.get('categoria', 'n/a')}. "
            f"Estilo: {item.get('estilo', 'n/a')}. "
            f"Color: {item.get('color', 'n/a')}. "
            f"Clima: {item.get('clima', 'n/a')}. "
            f"Ocasion: {item.get('ocasion', 'n/a')}."
        )
        descriptions.append(text_desc)
    return descriptions


def extract_with_backoff(img_path: Path, max_retries: int = 3) -> list[str]:
    for attempt in range(max_retries):
        try:
            return extract_garments_metadata(img_path)
        except Exception as e:
            if "429" in str(e) or "quota" in str(e).lower():
                wait_time = 30 * (attempt + 1)
                logger.warning("  [!] Limite API. Esperando %ds (intento %d/%d)...", wait_time, attempt + 1, max_retries)
                time.sleep(wait_time)
            else:
                logger.error("  [X] Error en %s: %s", img_path.name, e)
                return []
    return []


def get_embedding(text: str) -> list[float] | None:
    client = get_client()
    try:
        response = client.models.embed_content(
            model=EMBEDDING_MODEL_NAME,
            contents=text,
            config=types.EmbedContentConfig(
                task_type="SEMANTIC_SIMILARITY",
                output_dimensionality=EXPECTED_DIMS,
            ),
        )
        return response.embeddings[0].values
    except Exception as e:
        logger.error("  [X] Error al generar embedding: %s", e)
        return None


def run_pipeline_resilient(images: list[Path]):
    processed_data = load_checkpoint()
    all_vectors    = []

    logger.info("Imagenes en checkpoint: %d", len(processed_data))

    consecutive_failures        = 0
    images_processed_in_session = 0
    start_time                  = time.time()
    total_images                = len(images)
    status_final                = "completado"

    for i, img_path in enumerate(images, start=1):
        img_id = img_path.name

        if img_id in processed_data:
            all_vectors.extend(processed_data[img_id])
            continue

        descriptions = extract_with_backoff(img_path)

        if not descriptions:
            consecutive_failures += 1
            logger.warning("  -> Fallo detectado. Consecutivos: %d/3", consecutive_failures)
            if consecutive_failures >= 3:
                logger.error("\n[!!!] 3 FALLOS CONSECUTIVOS. Abortando.")
                status_final = "parcial_por_errores"
                break
            continue
        else:
            consecutive_failures = 0

        img_vectors = []
        for desc in descriptions:
            vector = get_embedding(desc)
            if vector:
                img_vectors.append(vector)
                all_vectors.append(vector)

        if img_vectors:
            processed_data[img_id] = img_vectors
            save_checkpoint(processed_data)

        images_processed_in_session += 1
        elapsed     = time.time() - start_time
        avg_time    = elapsed / images_processed_in_session
        remaining   = total_images - i
        eta_seconds = avg_time * remaining

        logger.info(
            "[%d/%d] %s procesada | Transcurrido: %s | ETA: %s",
            i, total_images, img_id, format_time(elapsed), format_time(eta_seconds),
        )

        time.sleep(4.5)

    if not all_vectors:
        logger.error("No se recolectaron vectores. Abortando.")
        sys.exit(1)

    matrix     = np.array(all_vectors, dtype=np.float32)
    avg_vector = matrix.mean(axis=0)

    generate_metadata_report(len(processed_data), len(all_vectors), status_final)

    return avg_vector, len(all_vectors)


def _generate_internal_jwt() -> str:
    now = datetime.datetime.utcnow()
    payload = {
        "sub":  INTERNAL_SERVICE_NAME,
        "role": "INTERNAL_SERVICE",
        "iat":  now,
        "exp":  now + datetime.timedelta(minutes=5),
    }
    return jwt.encode(payload, INTERNAL_JWT_SECRET, algorithm="HS256")


def save_to_back(vector: np.ndarray, count: int, description: str) -> None:
    token = _generate_internal_jwt()

    payload = {
        "avgVector":   vector.tolist(),
        "imageCount":  count,
        "modelUsed":   EMBEDDING_MODEL_NAME,
        "description": description,
    }

    headers = {
        "Content-Type":  "application/json",
        "Authorization": f"Bearer {token}",
    }

    logger.info("\n[+] Enviando vector a dressme-back -> %s (imageCount=%d)", TREND_DATASET_ENDPOINT, count)

    try:
        response = requests.post(TREND_DATASET_ENDPOINT, data=json.dumps(payload), headers=headers, timeout=30)
        response.raise_for_status()
        result = response.json()
        logger.info("[+] Vector persistido — id=%s, computedAt=%s", result.get("id"), result.get("computedAt"))
    except requests.exceptions.ConnectionError:
        raise RuntimeError(f"Conexion rechazada a {TREND_DATASET_ENDPOINT}")
    except requests.exceptions.HTTPError as e:
        raise RuntimeError(f"Error HTTP {e.response.status_code} al persistir el vector")
    except requests.exceptions.Timeout:
        raise RuntimeError("Timeout al conectar con dressme-back")


def main():
    parser = argparse.ArgumentParser(description="Generador Batch de Vector de Tendencia")
    parser.add_argument("--path", type=str, default="dataset_moda_actual")
    parser.add_argument("--desc", type=str, default="Tendencia extraida por atributos")
    args = parser.parse_args()

    if not os.environ.get("GEMINI_API_KEY"):
        logger.error("Falta GEMINI_API_KEY en las variables de entorno.")
        sys.exit(1)

    get_client()

    logger.info("=== INICIANDO PROCESAMIENTO BATCH ===")
    logger.info("Dataset  : %s", args.path)
    logger.info("Backend  : %s", TREND_DATASET_ENDPOINT)

    image_paths = discover_images(Path(args.path))
    logger.info("Total de imagenes a procesar: %d\n", len(image_paths))

    avg_vec, total_items = run_pipeline_resilient(image_paths)
    save_to_back(avg_vec, total_items, args.desc)

    logger.info("\n=== PROCESO COMPLETADO ===")


if __name__ == "__main__":
    main()
