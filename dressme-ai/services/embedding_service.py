"""
services/embedding_service.py
──────────────────────────────
Genera embeddings de texto usando Google Gemini gemini-embedding-001.

Responsabilidades:
  - Llamar a la API de Gemini en modo batch (una sola llamada para N tarjetas)
  - Fijar la dimensión de salida en 1536 via MRL (Matryoshka Representation Learning)
    para mantener compatibilidad con el schema vector(1536) existente en DB
  - Mapear la respuesta al schema interno StyleCardEmbeddingResult
  - Manejar errores de la API con mensajes claros

"""

import logging
import google.generativeai as genai
from google.api_core.exceptions import GoogleAPIError, ResourceExhausted, Unauthenticated
from schemas.onboarding import (
    GenerateEmbeddingsRequest,
    GenerateEmbeddingsResponse,
    StyleCardEmbeddingResult,
)

logger = logging.getLogger(__name__)

EMBEDDING_MODEL = "models/gemini-embedding-001"
EXPECTED_DIMENSIONS = 1536


class EmbeddingService:

    def __init__(self, api_key: str):
        """
        Recibe la API key por inyección desde dependencies.py.
        Configura el cliente de Gemini una sola vez al construir el servicio.
        """
        genai.configure(api_key=api_key)

    def generate_for_style_cards(
        self, request: GenerateEmbeddingsRequest
    ) -> GenerateEmbeddingsResponse:
        """
        Genera embeddings para una lista de style cards.

        La Gemini API acepta una lista de strings en el campo `contents`.
        Tiene la forma:
            result.embeddings[i].values  → list[float] del i-ésimo texto

        output_dimensionality=1536 activa MRL para fijar la dimensión de salida.
        """
        logger.info(
            "EmbeddingService: Generando embeddings para %d tarjetas con modelo %s",
            len(request.style_cards),
            EMBEDDING_MODEL,
        )

        texts = [card.semantic_description for card in request.style_cards]

        # ── Llamada batch a Gemini ────────────────────────────────────────────
        # embed_content acepta una lista de strings directamente.
        # task_type="SEMANTIC_SIMILARITY" optimiza los vectores para comparar
        # similitud entre textos — exactamente lo que necesitamos para comparar
        # el taste_vector del usuario con los embeddings de sus prendas.
        result = genai.embed_content(
            model=EMBEDDING_MODEL,
            content=texts,
            task_type="SEMANTIC_SIMILARITY",
            output_dimensionality=EXPECTED_DIMENSIONS,
        )

        # ── Validación de la respuesta ────────────────────────────────────────
        if len(result["embedding"]) != len(request.style_cards):
            raise ValueError(
                f"Gemini devolvió {len(result['embedding'])} embeddings "
                f"pero se enviaron {len(request.style_cards)} textos. "
                "La respuesta está incompleta."
            )

        # ── Mapeo a schema interno ────────────────────────────────────────────
        # result["embedding"] es una lista de listas de floats, en el mismo
        # orden que los textos enviados.
        results: list[StyleCardEmbeddingResult] = []

        for card, vector in zip(request.style_cards, result["embedding"]):

            if len(vector) != EXPECTED_DIMENSIONS:
                raise ValueError(
                    f"Embedding para tarjeta '{card.id}' tiene {len(vector)} dims, "
                    f"se esperaban {EXPECTED_DIMENSIONS}. "
                    f"Verifica output_dimensionality en la llamada al modelo."
                )

            results.append(
                StyleCardEmbeddingResult(
                    id=card.id,
                    embedding_vector=vector,
                )
            )
            logger.debug("EmbeddingService: Embedding generado para tarjeta %s", card.id)

        logger.info(
            "EmbeddingService: %d embeddings generados correctamente", len(results)
        )

        return GenerateEmbeddingsResponse(
            embeddings=results,
            model_used=EMBEDDING_MODEL,
            total_processed=len(results),
        )