import logging
import json
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import JSONResponse

from schemas.wardrobe import VisionAnalysisRequest, VisionAnalysisResponse
from services.wardrobe import WardrobeAnalysisService
from settings.dependencies import get_wardrobe_analysis_service

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/ai/wardrobe",
    tags=["Wardrobe AI"],
)


@router.post(
    "/analyze",
    status_code=status.HTTP_200_OK,
    summary="Analizar prenda con Gemini Vision",
    description="""
    Recibe el ID de la prenda y la URL interna de su imagen.
    Gemini Vision analiza la imagen en un solo paso y devuelve:

    - **Categoría, estilo, clima y ocasión**: UUIDs del catálogo activo de dressme.
    - **Color detectado**: UUID del color más cercano del catálogo +
      valores HSL exactos del color dominante de la prenda.
    - **confidence_score**: confianza de Gemini en el análisis [0.00–1.00].

    Los valores HSL permiten al scoring engine calcular armonía cromática real
    entre prendas (complementarios, análogos, triádicos) en lugar de comparar
    solo UUIDs de color.

    **Error:** si Gemini no puede analizar la imagen, devuelve HTTP 503.
    El backend reintenta o notifica al usuario para que corrija la prenda manualmente.

    Consumido exclusivamente por **dressme-back** (red interna Docker).
    """,
)
def analyze_clothing(
    request: VisionAnalysisRequest,
    service: WardrobeAnalysisService = Depends(get_wardrobe_analysis_service),
):
    logger.info(
        "Router: POST /ai/wardrobe/analyze — prenda=%s", request.clothing_id
    )

    try:
        result = service.analyze(request)
    except Exception as e:
        logger.error(
            "Router: Análisis fallido para prenda %s: %s",
            request.clothing_id, e,
        )
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Vision analysis failed for clothing {request.clothing_id}: {e}",
        )

    payload_json = result.model_dump_json()
    logger.info(
        "Router: Análisis completado — prenda=%s, categoría=%s, "
        "color=HSL(%d,%d,%d), confidence=%.4f",
        request.clothing_id,
        result.predicted_category_id,
        result.detected_color_hsl.hue,
        result.detected_color_hsl.saturation,
        result.detected_color_hsl.lightness,
        result.confidence_score,
    )
    logger.info("Router: Payload enviado a dressme-back: %s", payload_json)

    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content=json.loads(payload_json),
    )