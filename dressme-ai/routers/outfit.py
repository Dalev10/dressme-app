"""
routers/outfit.py
──────────────────
Endpoints del flujo de generación de outfits.

Rutas expuestas (consumidas exclusivamente por dressme-back en la red interna Docker):

  POST /ai/outfit/embed-clothing        → vectoriza una prenda individual
  POST /ai/outfit/embed-clothing/batch  → vectoriza N prendas en una sola llamada
  POST /ai/outfit/generate              → genera combinaciones candidatas del guardarropa
  POST /ai/outfit/generate/orchestrated → genera candidatos con dresscode_score calculado

Patrón de diseño:
  - El router no contiene lógica de negocio.
  - Todo el procesamiento está en los services inyectados via Depends.
  - Logging estructurado en cada endpoint: entrada y salida con métricas clave.
  - Los errores de la API de Gemini se propagan sin atrapar — el error_handler
    global de infra/error_handler.py los convierte en respuestas HTTP apropiadas.
"""

import logging
from fastapi import APIRouter, Depends, status

from schemas.outfit import (
    ClothingEmbeddingRequest,
    ClothingEmbeddingResponse,
    OutfitGenerationRequest,
    OutfitGenerationResponse,
    OutfitGenerationOrchestratedRequest,
    OutfitGenerationOrchestratedResponse,
    ScoredOutfitCandidate,
)
from services.embedding_clothing_service import EmbeddingClothingService
from services.outfit_generator_service import OutfitGeneratorService
from services.dresscode_similarity_service import DresscodeSimilarityService
from settings.dependencies import (
    get_embedding_clothing_service,
    get_outfit_generator_service,
    get_dresscode_similarity_service,
)

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/ai/outfit",
    tags=["Outfit AI"],
)


# ── Endpoint 1: Embed single clothing ─────────────────────────────────────────

@router.post(
    "/embed-clothing",
    response_model=ClothingEmbeddingResponse,
    status_code=status.HTTP_200_OK,
    summary="Vectorizar una prenda",
    description="""
    Recibe los atributos semánticos de una prenda y genera su embedding de 1536 dimensiones
    usando Gemini `gemini-embedding-001`.

    El texto semántico enviado al modelo se construye como:
    `"{category} {style} {color} para {weather} y {occasion}"`

    El embedding resultante debe ser persistido por **dressme-back** vía
    `PATCH /internal/wardrobe/{clothingId}/embedding` en dressme-database.

    **Cuándo se usa este endpoint:**
    - Vectorización individual tras una corrección manual del usuario
      (la prenda quedó con `isEmbeddingStale = true`).
    - Para un volumen bajo de prendas nuevas sin embedding.

    Para lotes grandes, usar `/embed-clothing/batch`.

    Consumido exclusivamente por **dressme-back** (red interna Docker).
    """,
)
def embed_clothing(
    request: ClothingEmbeddingRequest,
    service: EmbeddingClothingService = Depends(get_embedding_clothing_service),
) -> ClothingEmbeddingResponse:
    logger.info(
        "Router: POST /ai/outfit/embed-clothing — prenda=%s categoria=%s estilo=%s",
        request.clothing_id,
        request.category,
        request.style,
    )

    result = service.embed(request)

    logger.info(
        "Router: Embedding generado para prenda=%s (dims=%d)",
        result.clothing_id,
        len(result.embedding_vector),
    )
    return result


# ── Endpoint 2: Embed batch ───────────────────────────────────────────────────

@router.post(
    "/embed-clothing/batch",
    response_model=list[ClothingEmbeddingResponse],
    status_code=status.HTTP_200_OK,
    summary="Vectorizar prendas en batch",
    description="""
    Recibe una lista de prendas y genera sus embeddings en **una sola llamada** a Gemini,
    optimizando el uso de cuota de la API.

    **Cuándo se usa este endpoint:**
    - Prerequisito del flujo de generación de outfits: vectorizar todas las prendas
      del guardarropa que tengan `embeddingVector = null` o `isEmbeddingStale = true`.
    - El orquestador `OutfitOrchestratorServiceImpl` llama a este endpoint antes
      de llamar a `/generate`.

    Cada embedding resultante debe ser persistido por **dressme-back** en
    `tbl_clothes.embedding_vector` via `PATCH /internal/wardrobe/{clothingId}/embedding`.

    Consumido exclusivamente por **dressme-back** (red interna Docker).
    """,
)
def embed_clothing_batch(
    requests: list[ClothingEmbeddingRequest],
    service: EmbeddingClothingService = Depends(get_embedding_clothing_service),
) -> list[ClothingEmbeddingResponse]:
    logger.info(
        "Router: POST /ai/outfit/embed-clothing/batch — %d prendas a vectorizar",
        len(requests),
    )

    if not requests:
        logger.warning("Router: /embed-clothing/batch recibió lista vacía.")
        return []

    results = service.embed_batch(requests)

    logger.info(
        "Router: Batch completado — %d embeddings generados de %d solicitados",
        len(results),
        len(requests),
    )
    return results


# ── Endpoint 3: Generate outfit candidates ────────────────────────────────────

@router.post(
    "/generate",
    response_model=OutfitGenerationResponse,
    status_code=status.HTTP_200_OK,
    summary="Generar combinaciones candidatas de outfits",
    description="""
    Recibe el guardarropa del usuario (prendas ya vectorizadas con sus slots y HSL resueltos)
    y genera hasta **20 combinaciones candidatas** listas para scoring.

    **Prerrequisito:** todas las prendas en `wardrobe` deben tener `embeddingVector != null`.
    dressme-back garantiza esto llamando primero a `/embed-clothing/batch`.

    **Algoritmo:**
    1. Filtra prendas con embedding válido (ignora las que lleguen sin él).
    2. Agrupa por slot: TOP, BOTTOM, OUTERWEAR, FOOTWEAR, ONEPIECE.
    3. Selecciona top-5 por slot (aleatorio, el scoring rankea después).
    4. Genera el producto cartesiano de slots:
       - **Separates:** TOP × BOTTOM × (OUTERWEAR?) × (FOOTWEAR?)
       - **Onepiece:** ONEPIECE × (OUTERWEAR?) × (FOOTWEAR?)
    5. Mezcla aleatoriamente y limita a 20 candidatos.
    6. Calcula el `outfit_vector` = media de los embeddings de las prendas del outfit.

    **Respuesta:**
    - `candidates`: lista de outfits con slots + outfit_vector.
    - `total_candidates`: combinaciones generadas antes del límite de 20.
    - `skipped_clothing`: prendas ignoradas por embedding nulo o slot inválido.

    El scoring (colorScore, dresscodeScore, tasteScore) lo calcula **dressme-back**
    con `ColorScoreService`, `TasteSimilarityService` y `ScoreEngineService`.

    Consumido exclusivamente por **dressme-back** (red interna Docker).
    """,
)
def generate_outfits(
    request: OutfitGenerationRequest,
    service: OutfitGeneratorService = Depends(get_outfit_generator_service),
) -> OutfitGenerationResponse:
    logger.info(
        "Router: POST /ai/outfit/generate — usuario=%s | ocasión=%s | clima=%s "
        "| dress_code=%s | prendas=%d",
        request.user_id,
        request.occasion,
        request.weather,
        request.dress_code or "N/A",
        len(request.wardrobe),
    )

    result = service.generate(request)

    logger.info(
        "Router: Generación completada — candidatos=%d (de %d posibles) | ignoradas=%d",
        len(result.candidates),
        result.total_candidates,
        result.skipped_clothing,
    )
    return result


# ── Endpoint 4: Generate orchestrated outfit candidates ───────────────────────

@router.post(
    "/generate/orchestrated",
    response_model=OutfitGenerationOrchestratedResponse,
    status_code=status.HTTP_200_OK,
    summary="Generar candidatos orquestados con dresscode_score",
    description="""
    Recibe el guardarropa pre-filtrado del usuario y el embedding del dress code
    seleccionado (opcional). Devuelve candidatos de outfit enriquecidos con
    `dresscode_score` calculado por similitud coseno.

    **Flujo:**
    1. Genera combinaciones candidatas usando `OutfitGeneratorService`.
    2. Para cada candidato, calcula `dresscode_score` con `DresscodeSimilarityService`.
    3. Devuelve `ScoredOutfitCandidate[]` con `dresscode_score` poblado.

    **Responsabilidad de dressme-back tras recibir la respuesta:**
    - Calcular `color_score`, `taste_score`, `trend_score` y `total_score`.
    - Persistir cada outfit en dressme-database.

    **Dress code opcional:**
    Si `dresscode_embedding` es `null`, `dresscode_score = 0.0` en todos los
    candidatos y el ScoreEngine en dressme-back redistribuirá el peso del componente.

    Consumido exclusivamente por **dressme-back** (red interna Docker).
    """,
)
def generate_outfits_orchestrated(
    request:   OutfitGenerationOrchestratedRequest,
    generator: OutfitGeneratorService     = Depends(get_outfit_generator_service),
    dresscode: DresscodeSimilarityService = Depends(get_dresscode_similarity_service),
) -> OutfitGenerationOrchestratedResponse:
    logger.info(
        "Router: POST /ai/outfit/generate/orchestrated — "
        "userId=%s | ocasión=%s | clima=%s | prendas=%d | dresscode=%s",
        request.user_id,
        request.occasion,
        request.weather,
        len(request.wardrobe),
        "sí" if request.dresscode_embedding is not None else "no",
    )

    # ── Paso 1: delegar al generador usando el contrato existente ─────────────
    generation_request = OutfitGenerationRequest(
        user_id=request.user_id,
        occasion=request.occasion,
        weather=request.weather,
        wardrobe=request.wardrobe,
        dress_code=None,
    )

    generation_result = generator.generate(generation_request)

    logger.info(
        "Router: %d candidatos crudos generados (total=%d, ignoradas=%d)",
        len(generation_result.candidates),
        generation_result.total_candidates,
        generation_result.skipped_clothing,
    )

    # ── Paso 2 + 3: calcular dresscode_score y proyectar a ScoredOutfitCandidate ──
    scored_candidates: list[ScoredOutfitCandidate] = []

    for candidate in generation_result.candidates:
        similarity = dresscode.compute(
            outfit_vector=candidate.outfit_vector,
            dresscode_embedding=request.dresscode_embedding,
        )
        scored_candidates.append(
            ScoredOutfitCandidate(
                slots=candidate.slots,
                outfit_vector=candidate.outfit_vector,
                dresscode_score=similarity.score,
            )
        )

    logger.info(
        "Router: %d candidatos enriquecidos con dresscode_score (applies=%s). "
        "Retornando a dressme-back.",
        len(scored_candidates),
        "True" if request.dresscode_embedding is not None else "False",
    )

    return OutfitGenerationOrchestratedResponse(
        candidates=scored_candidates,
        total_candidates=generation_result.total_candidates,
        skipped_clothing=generation_result.skipped_clothing,
    )