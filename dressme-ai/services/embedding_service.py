"""
services/embedding_service.py
──────────────────────────────
Genera embeddings de texto usando OpenAI text-embedding-3-small.

Responsabilidades:
  - Llamar a la API de OpenAI en modo batch (una sola llamada para N tarjetas)
  - Mapear la respuesta al schema interno StyleCardEmbeddingResult
  - Manejar errores de la API con mensajes claros

Por qué text-embedding-3-small y no large:
  - small  → 1536 dims, ~6x más barato, latencia menor. Suficiente para moda.
  - large  → 3072 dims, más preciso para semántica muy fina (código, ciencia).
  El taste_vector ya está definido como vector(1536) en DB, así que small es
  el modelo correcto y no hay que cambiar el schema.
"""

import logging
from openai import OpenAI
from schemas.onboarding import (
    GenerateEmbeddingsRequest,
    GenerateEmbeddingsResponse,
    StyleCardEmbeddingResult,
)

logger = logging.getLogger(__name__)

EMBEDDING_MODEL = "text-embedding-3-small"
EXPECTED_DIMENSIONS = 1536


class EmbeddingService:

    def __init__(self, openai_client: OpenAI):
        """
        Recibe el cliente de OpenAI por inyección.
        Esto facilita el testing: en tests se puede pasar un cliente mock
        sin tocar variables de entorno ni hacer llamadas reales.
        """
        self.client = openai_client

    def generate_for_style_cards(
        self, request: GenerateEmbeddingsRequest
    ) -> GenerateEmbeddingsResponse:
        """
        Genera embeddings para una lista de style cards en una sola llamada batch.

        La API de OpenAI acepta una lista de strings en el campo `input`.
        Devuelve los embeddings en el mismo orden, así que podemos hacer
        zip(request.style_cards, response.data) de forma segura.
        """
        logger.info(
            "EmbeddingService: Generando embeddings para %d tarjetas con modelo %s",
            len(request.style_cards),
            EMBEDDING_MODEL,
        )

        # Extraer los textos en el mismo orden que las tarjetas
        texts = [card.semantic_description for card in request.style_cards]

        # ── Llamada batch a OpenAI ────────────────────────────────────────────
        # Una sola llamada para todas las tarjetas. Mucho más eficiente
        # que N llamadas individuales, y más barato en términos de tokens.
        response = self.client.embeddings.create(
            model=EMBEDDING_MODEL,
            input=texts,
        )

        # ── Validación de la respuesta ────────────────────────────────────────
        if len(response.data) != len(request.style_cards):
            raise ValueError(
                f"OpenAI devolvió {len(response.data)} embeddings "
                f"pero se enviaron {len(request.style_cards)} textos. "
                "La respuesta está incompleta."
            )

        # ── Mapeo a schema interno ────────────────────────────────────────────
        # response.data está ordenado por índice, igual que request.style_cards
        results: list[StyleCardEmbeddingResult] = []

        for card, embedding_obj in zip(request.style_cards, response.data):
            vector = embedding_obj.embedding

            # Sanidad: OpenAI siempre devuelve 1536 para este modelo,
            # pero lo validamos explícitamente para detectar cambios de API.
            if len(vector) != EXPECTED_DIMENSIONS:
                raise ValueError(
                    f"Embedding para tarjeta '{card.id}' tiene {len(vector)} dims, "
                    f"se esperaban {EXPECTED_DIMENSIONS}. "
                    f"¿Cambió el modelo de {EMBEDDING_MODEL}?"
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