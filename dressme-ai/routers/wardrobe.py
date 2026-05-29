import logging
import json
from fastapi import APIRouter, Depends, status
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

    **Fallback:** si Gemini no puede clasificar la imagen, la prenda se asigna
    a "Uncategorized" con confidence_score=0.0. El usuario puede corregirla manualmente.

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

    result = service.analyze(request)
    payload_json = result.model_dump_json()
    logger.info("AI-Router: Payload enviado a dressme-back: %s", payload_json)

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
    
    # Devolver JSON aplanado usando JSONResponse
    payload_json = result.model_dump_json()
    logger.info("Router: Payload enviado a dressme-back: %s", payload_json)
    
    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content=json.loads(payload_json)
    )