"""
services/embedding_service.py
──────────────────────────────
Genera embeddings de texto usando el SDK oficial de Google Gen AI (google-genai).
"""

import logging

from google import genai
from google.genai import types

from schemas.onboarding import (
    GenerateEmbeddingsRequest,
    GenerateEmbeddingsResponse,
    StyleCardEmbeddingResult,
)

logger = logging.getLogger(__name__)

EMBEDDING_MODEL = "gemini-embedding-001"
EXPECTED_DIMENSIONS = 1536


class EmbeddingService:
    def __init__(self, api_key: str):
        self.client = genai.Client(api_key=api_key)

    def generate_for_style_cards(
        self, request: GenerateEmbeddingsRequest
    ) -> GenerateEmbeddingsResponse:
        if not request.style_cards:
            raise ValueError("No se enviaron style cards para generar embeddings.")

        logger.info(
            "EmbeddingService: Generando embeddings para %d tarjetas con modelo %s",
            len(request.style_cards),
            EMBEDDING_MODEL,
        )

        texts = [card.semantic_description for card in request.style_cards]

        try:
            response = self.client.models.embed_content(
                model=EMBEDDING_MODEL,
                contents=texts,
                config=types.EmbedContentConfig(
                    task_type="SEMANTIC_SIMILARITY",
                    output_dimensionality=EXPECTED_DIMENSIONS,
                ),
            )
        except Exception as exc:
            logger.exception("EmbeddingService: error llamando a Gemini")
            raise RuntimeError(f"Error generando embeddings con Gemini: {exc}") from exc

        embeddings_list = response.embeddings
        if not embeddings_list:
            raise ValueError("Gemini no devolvió embeddings.")

        if len(embeddings_list) != len(request.style_cards):
            raise ValueError(
                f"Gemini devolvió {len(embeddings_list)} embeddings pero se enviaron "
                f"{len(request.style_cards)} textos. La respuesta está incompleta."
            )

        results: list[StyleCardEmbeddingResult] = []

        for card, embedding_obj in zip(request.style_cards, embeddings_list):
            vector = embedding_obj.values

            if vector is None:
                raise ValueError(f"El embedding de la tarjeta '{card.id}' vino vacío.")

            if len(vector) != EXPECTED_DIMENSIONS:
                raise ValueError(
                    f"Embedding para tarjeta '{card.id}' tiene {len(vector)} dims, "
                    f"se esperaban {EXPECTED_DIMENSIONS}."
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
