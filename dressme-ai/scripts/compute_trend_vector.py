"""
scripts/compute_trend_vector.py  — VERSIÓN REFACTORIZADA
──────────────────────────────────────────────────────────
SDK: google-genai==2.7.0  (nueva API — from google import genai)

Script batch que:
  1. Lee todas las imágenes de Dataset_moda_actual.
  2. Extrae atributos de cada prenda con Gemini Vision (gemini-2.5-flash).
  3. Genera embeddings de texto con gemini-embedding-001 (1536 dims).
  4. Calcula el vector promedio de todos los embeddings.
  5. Persiste el vector llamando al endpoint REST de dressme-back.

Variables de entorno requeridas:
    GEMINI_API_KEY      → API key de Google Gemini
    BACK_URL            → URL base de dressme-back (default: http://dressme-back:8080)
    INTERNAL_JWT_SECRET → Secret JWT compartido con dressme-back

Ejecución:
    python scripts/compute_trend_vector.py --path dataset_moda_actual --desc "Primera carga"
"""

import argparse
import datetime
import json
import logging
import os
import sys
import time
from pathlib import Path

import numpy as np
import requests
import google.generativeai as genai
from jose import jwt
from PIL import Image
from dotenv import load_dotenv, find_dotenv

# Cargar variables de entorno buscando el archivo .env hacia arriba en el árbol de directorios
load_dotenv(find_dotenv())

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(message)s",
)
logger = logging.getLogger(__name__)

# ── Constantes ────────────────────────────────────────────────────────────────

SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
CHECKPOINT_FILE      = Path("checkpoint_prendas.json")
METADATA_FILE        = Path("trend_metadata_report.json")

VISION_MODEL_NAME    = "gemini-2.5-flash"
EMBEDDING_MODEL_NAME = "models/embedding-001"
EXPECTED_DIMS        = 1536

# ── Configuración REST ────────────────────────────────────────────────────────

BACK_URL               = os.environ.get("BACK_URL", "http://dressme-back:8080")
TREND_DATASET_ENDPOINT = f"{BACK_URL}/internal/trend-dataset/config"
INTERNAL_JWT_SECRET    = os.environ.get(
    "INTERNAL_JWT_SECRET",
    os.environ.get(
        "JWT_SECRET",
        "dressme-secret-key-change-in-production-minimum-256-bits-required-12345"
    )
)
INTERNAL_SERVICE_NAME  = "dressme-ai-script"

# ── Cliente Gemini (singleton) ────────────────────────────────────────────────
# Con google-generativeai, se usa genai.configure() una sola vez.

_gemini_configured: bool = False

def get_gemini_client():
    global _gemini_configured
    if not _gemini_configured:
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            logger.error("Falta GEMINI_API_KEY en las variables de entorno.")
            sys.exit(1)
        genai.configure(api_key=api_key)
        _gemini_configured = True

# ── Utilidades ────────────────────────────────────────────────────────────────

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

# ── Descubrimiento de imágenes ────────────────────────────────────────────────

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

# ── Extracción con Gemini Vision ──────────────────────────────────────────────

def extract_garments_metadata(image_path: Path) -> list[str]:
    """
    Extrae atributos de las prendas de una imagen usando Gemini Vision.
    """
    get_gemini_client()
    
    model = genai.GenerativeModel(model_name=VISION_MODEL_NAME)

    prompt = (
        "Analiza esta imagen de moda e identifica cada prenda de ropa individual. "
        "Para cada prenda, extrae exactamente estos 5 atributos: categoria, estilo, color, clima y ocasion. "
        "Devuelve la respuesta estrictamente como un arreglo JSON de objetos. "
        "Ejemplo: [{'categoria': 'pantalones', 'estilo': 'urbano', 'color': 'azul', 'clima': 'templado', 'ocasion': 'casual'}]"
    )

    # Leer imagen como bytes
    with open(image_path, "rb") as f:
        image_bytes = f.read()

    suffix = image_path.suffix.lower()
    mime_map = {
        ".jpg":  "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png":  "image/png",
        ".webp": "image/webp",
    }
    mime_type = mime_map.get(suffix, "image/jpeg")

    response = model.generate_content(
        [
            {
                "mime_type": mime_type,
                "data": image_bytes
            },
            prompt
        ]
    )

    items = json.loads(response.text)
    descriptions = []
    for item in items:
        text_desc = (
            f"Categoría: {item.get('categoria', 'n/a')}. "
            f"Estilo: {item.get('estilo', 'n/a')}. "
            f"Color: {item.get('color', 'n/a')}. "
            f"Clima: {item.get('clima', 'n/a')}. "
            f"Ocasión: {item.get('ocasion', 'n/a')}."
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
                logger.warning(
                    "  [!] Límite API. Esperando %ds (intento %d/%d)...",
                    wait_time, attempt + 1, max_retries,
                )
                time.sleep(wait_time)
            else:
                logger.error("  [X] Error en %s: %s", img_path.name, e)
                return []
    return []


def get_embedding(text: str) -> list[float] | None:
    """
    Genera embedding de texto usando google-generativeai.
    """
    get_gemini_client()
    try:
        response = genai.embed_content(
            model=EMBEDDING_MODEL_NAME,
            content=text,
        )
        return response['embedding']
    except Exception as e:
        logger.error("  [X] Error al generar embedding: %s", e)
        return None

# ── Pipeline principal ────────────────────────────────────────────────────────

def run_pipeline_resilient(images: list[Path]):
    processed_data = load_checkpoint()
    all_vectors    = []

    logger.info("Imágenes en checkpoint: %d", len(processed_data))

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
                logger.error("\n[!!!] 3 FALLOS CONSECUTIVOS. Abortando peticiones a la API.")
                logger.error("Generando vector promedio con los datos obtenidos hasta el momento...")
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

        time.sleep(4.5)  # Throttling: respetar 15 RPM

    if not all_vectors:
        logger.error("No se recolectaron vectores suficientes para promediar. Abortando.")
        sys.exit(1)

    matrix     = np.array(all_vectors, dtype=np.float32)
    avg_vector = matrix.mean(axis=0)

    generate_metadata_report(len(processed_data), len(all_vectors), status_final)

    return avg_vector, len(all_vectors)

# ── Persistencia vía REST ─────────────────────────────────────────────────────

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

    logger.info(
        "\n[+] Enviando vector a dressme-back → %s (imageCount=%d)",
        TREND_DATASET_ENDPOINT,
        count,
    )

    try:
        response = requests.post(
            TREND_DATASET_ENDPOINT,
            data=json.dumps(payload),
            headers=headers,
            timeout=30,
        )
        response.raise_for_status()

        result = response.json()
        logger.info(
            "[+] Vector persistido por dressme-database — id=%s, computedAt=%s",
            result.get("id"),
            result.get("computedAt"),
        )

    except requests.exceptions.ConnectionError:
        logger.error(
            "[X] No se pudo conectar a dressme-back en %s. ¿Está el servicio corriendo?",
            TREND_DATASET_ENDPOINT,
        )
        raise RuntimeError(f"Conexión rechazada a {TREND_DATASET_ENDPOINT}")

    except requests.exceptions.HTTPError as e:
        logger.error(
            "[X] dressme-back rechazó la request — status=%d, body=%s",
            e.response.status_code,
            e.response.text,
        )
        raise RuntimeError(
            f"Error HTTP {e.response.status_code} al persistir el vector de tendencia"
        )

    except requests.exceptions.Timeout:
        logger.error("[X] Timeout esperando respuesta de dressme-back (30s).")
        raise RuntimeError("Timeout al conectar con dressme-back")

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Generador Batch de Vector de Tendencia")
    parser.add_argument("--path", type=str, default="dataset_moda_actual", help="Ruta de imágenes")
    parser.add_argument("--desc", type=str, default="Tendencia extraída por atributos", help="Descripción del dataset")
    args = parser.parse_args()

    # Verificar que la API key existe antes de arrancar
    if not os.environ.get("GEMINI_API_KEY"):
        logger.error("Falta GEMINI_API_KEY en las variables de entorno.")
        sys.exit(1)

    # Inicializar el cliente (valida la key al arrancar)
    get_gemini_client()

    logger.info("=== INICIANDO PROCESAMIENTO BATCH ===")
    logger.info("Dataset  : %s", args.path)
    logger.info("Backend  : %s", TREND_DATASET_ENDPOINT)

    image_paths = discover_images(Path(args.path))
    logger.info("Total de imágenes a procesar: %d\n", len(image_paths))

    avg_vec, total_items = run_pipeline_resilient(image_paths)

    save_to_back(avg_vec, total_items, args.desc)

    logger.info("\n=== PROCESO COMPLETADO ===")


if __name__ == "__main__":
    main()