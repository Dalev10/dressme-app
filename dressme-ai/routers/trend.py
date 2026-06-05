"""
routers/trend.py
─────────────────
Expone el endpoint POST /internal/ai/trend/score.

Consumido exclusivamente por dressme-back durante el flujo de puntuación
de outfits. No está expuesto públicamente a través del Gateway.
"""

import logging
from fastapi import APIRouter, Depends, status

from schemas.trend import TrendScoreRequest, TrendScoreResponse
from services.trend_score_service import TrendScoreService
from settings.dependencies import get_trend_score_service

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/ai/trend",
    tags=["Trend Score (Internal)"],
)


@router.post(
    "/score",
    response_model=TrendScoreResponse,
    status_code=status.HTTP_200_OK,
    summary="Calcular trend score de un outfit",
    description="""
    Recibe los embeddings de las prendas que componen un outfit y devuelve
    qué tan alineado está con las tendencias de moda actuales del dataset.

    **Algoritmo:**
    1. Promedia los embeddings de las prendas → `outfit_vector`.
    2. Carga el `avg_vector` del dataset desde `tbl_trend_dataset_config`.
    3. Calcula la similitud coseno entre ambos vectores.
    4. Normaliza a [0, 1] (similitudes negativas = 0).

    Si no hay vector de dataset disponible, devuelve `applies=false` para
    que el ScoreEngine redistribuya el peso del componente trend.

    **Consumido por:** `dressme-back → ScoreEngineService`
    """,
)
def score_trend(
    request: TrendScoreRequest,
    service: TrendScoreService = Depends(get_trend_score_service),
) -> TrendScoreResponse:
    logger.info(
        "Router: POST /internal/ai/trend/score — %d embeddings de prendas",
        len(request.outfit_embeddings),
    )
    result = service.score(request)
    logger.info(
        "Router: trend_score=%.4f applies=%s dataset_images=%d",
        result.trend_score,
        result.applies,
        result.dataset_images,
    )
    return result