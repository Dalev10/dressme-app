import logging
from fastapi import APIRouter, Depends, status

from schemas.onboarding import (
    ComputeTasteVectorRequest,
    ComputeTasteVectorResponse,
    GenerateEmbeddingsRequest,
    GenerateEmbeddingsResponse,
)
from services.taste_vector_service import TasteVectorService
from services.embedding_service import EmbeddingService
from settings.dependencies import get_taste_vector_service, get_embedding_service

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/ai/onboarding",
    tags=["Onboarding AI"],
)


# ── Endpoint 1: Compute Taste Vector ─────────────────────────────────────────

@router.post(
    "/compute-taste-vector",
    response_model=ComputeTasteVectorResponse,
    status_code=status.HTTP_200_OK,
    summary="Calcular taste vector del usuario",
    description="""
    Recibe las selecciones de onboarding del usuario (embeddings + reacciones)
    y devuelve el taste vector normalizado de 1536 dimensiones.

    Este endpoint es consumido exclusivamente por **dressme-back** durante el
    flujo `POST /api/v1/onboarding/calibrate`. El vector resultante es persistido
    por dressme-back vía PATCH a dressme-database.

    **Pesos aplicados por reacción:**
    - `LIKE` → +1.0
    - `DISLIKE` → -0.3
    - `SKIP` → 0.0 (no aporta información)
    """,
)
def compute_taste_vector(
    request: ComputeTasteVectorRequest,
    service: TasteVectorService = Depends(get_taste_vector_service),
) -> ComputeTasteVectorResponse:
    logger.info(
        "Router: POST /compute-taste-vector — usuario=%s, selecciones=%d",
        request.user_id,
        len(request.selections),
    )
    result = service.compute(request)
    logger.info("Router: Vector calculado exitosamente para usuario=%s", request.user_id)
    return result


# ── Endpoint 2: Generate Style Card Embeddings ───────────────────────────────

@router.post(
    "/generate-embeddings",
    response_model=GenerateEmbeddingsResponse,
    status_code=status.HTTP_200_OK,
    summary="Generar embeddings para style cards",
    description="""
    Recibe las descripciones semánticas de las style cards y genera sus embeddings
    usando OpenAI `text-embedding-3-small`.

    **Este endpoint se usa una sola vez** (o cuando se agregan tarjetas nuevas).
    Los embeddings generados deben persistirse en `tbl_style_cards.embedding_vector`
    para que el endpoint `compute-taste-vector` los reciba ya precalculados.

    Consumido por el script `scripts/seed_embeddings.py` de inicialización.
    """,
)
def generate_embeddings(
    request: GenerateEmbeddingsRequest,
    service: EmbeddingService = Depends(get_embedding_service),
) -> GenerateEmbeddingsResponse:
    logger.info(
        "Router: POST /generate-embeddings — %d tarjetas a procesar",
        len(request.style_cards),
    )
    result = service.generate_for_style_cards(request)
    logger.info(
        "Router: %d embeddings generados con modelo %s",
        result.total_processed,
        result.model_used,
    )
    return result