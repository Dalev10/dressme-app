"""
onboarding_router.py
─────────────────────
Router para el flujo de onboarding.

Expone endpoints para:
- Procesar las selecciones de estilo del usuario
- Generar y calcular el taste vector
"""

from fastapi import APIRouter, HTTPException
from pydantic import ValidationError

from schemas.onboarding import ComputeTasteVectorRequest, ComputeTasteVectorResponse
from services.taste_vector_service import TasteVectorService

router = APIRouter(
    prefix="/api/v1/onboarding",
    tags=["Onboarding"],
)

taste_vector_service = TasteVectorService()


@router.post("/compute-taste-vector", response_model=ComputeTasteVectorResponse)
def compute_taste_vector(request: ComputeTasteVectorRequest) -> ComputeTasteVectorResponse:
    """
    Calcula el taste vector del usuario basado en sus selecciones de onboarding.

    **Request:**
    - `user_id`: ID del usuario
    - `selections`: Lista de tarjetas con embedding y reacción (LIKE/DISLIKE/SKIP)

    **Response:**
    - `user_id`: ID del usuario
    - `taste_vector`: Vector de 1536 dimensiones (embedding del taste)
    - `cards_used`: Cantidad de tarjetas procesadas
    - `likes_count`: Cantidad de selecciones LIKE
    - `dislikes_count`: Cantidad de selecciones DISLIKE
    """
    try:
        taste_vector_result = taste_vector_service.compute(request)
        return taste_vector_result
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=f"Validación fallida: {str(e)}")
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error al calcular taste vector: {str(e)}",
        )
