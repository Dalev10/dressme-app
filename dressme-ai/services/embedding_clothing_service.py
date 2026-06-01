"""
services/embedding_clothing_service.py
────────────────────────────────────────
Genera embeddings semánticos para prendas del guardarropa.

Responsabilidades:
  - Construir el texto semántico de cada prenda combinando sus atributos:
      "{category} {style} {color} para {weather} y {occasion}"
  - Llamar a Gemini gemini-embedding-001 en modo batch (una sola llamada para N prendas)
  - Fijar la dimensión en 1536 via MRL (igual que EmbeddingService de onboarding)
  - Devolver cada ClothingEmbeddingResponse preservando el clothing_id original

Consistencia con el sistema:
  - Usa el mismo modelo (gemini-embedding-001) y dimensionalidad (1536) que los
    embeddings de style cards (EmbeddingService) y dress codes (sembrados en DB).
  - Esto garantiza que todos los vectores del sistema viven en el mismo espacio
    matemático, lo que hace válida la similitud coseno entre ellos.

Diferencia con EmbeddingService (onboarding):
  - EmbeddingService trabaja con StyleCardInput (id + semantic_description).
  - EmbeddingClothingService trabaja con ClothingEmbeddingRequest (id + 5 atributos),
    y construye el texto semántico internamente con una plantilla específica de prendas.
"""

import logging

import google.generativeai as genai
from google.api_core.exceptions import GoogleAPIError, ResourceExhausted, Unauthenticated

from schemas.outfit import ClothingEmbeddingRequest, ClothingEmbeddingResponse

logger = logging.getLogger(__name__)

# ── Constantes ────────────────────────────────────────────────────────────────

EMBEDDING_MODEL      = "models/gemini-embedding-001"
EXPECTED_DIMENSIONS  = 1536

# task_type SEMANTIC_SIMILARITY optimiza los vectores para comparar similitud
# entre textos — exactamente lo que se necesita para:
#   - taste_score:    coseno(outfit_vector, taste_vector)
#   - dresscode_score: coseno(outfit_vector, dressCode_embedding)
TASK_TYPE = "SEMANTIC_SIMILARITY"


class EmbeddingClothingService:
    """
    Genera embeddings semánticos para prendas usando Gemini gemini-embedding-001.

    Constructor:
      api_key → str, inyectado desde settings via dependencies.py.
                Configura el cliente de Gemini una sola vez al construir el servicio.
    """

    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        logger.info(
            "EmbeddingClothingService: Inicializado con modelo %s (%d dims)",
            EMBEDDING_MODEL,
            EXPECTED_DIMENSIONS,
        )

    # ── API pública ───────────────────────────────────────────────────────────

    def embed(self, request: ClothingEmbeddingRequest) -> ClothingEmbeddingResponse:
        """
        Vectoriza una sola prenda.
        Wrapper de embed_batch para casos en que dressme-back necesita
        vectorizar una prenda individualmente (ej: corrección manual).
        """
        results = self.embed_batch([request])
        return results[0]

    def embed_batch(
        self, requests: list[ClothingEmbeddingRequest]
    ) -> list[ClothingEmbeddingResponse]:
        """
        Vectoriza una lista de prendas en una sola llamada batch a Gemini.

        Proceso:
          1. Construir el texto semántico de cada prenda con _build_text().
          2. Llamar a genai.embed_content con todos los textos a la vez.
          3. Validar que la respuesta tenga el mismo número de vectores que textos.
          4. Validar que cada vector tenga exactamente 1536 dimensiones.
          5. Mapear cada vector a ClothingEmbeddingResponse preservando clothing_id.

        Lanza:
          ValueError          → si la respuesta de Gemini es inconsistente.
          ResourceExhausted   → si se agotó la cuota de la API (propagado al router).
          Unauthenticated     → si la API key no es válida.
          GoogleAPIError      → para cualquier otro error de la API de Google.
        """
        if not requests:
            logger.warning("EmbeddingClothingService: embed_batch recibió lista vacía.")
            return []

        logger.info(
            "EmbeddingClothingService: Vectorizando %d prendas con modelo %s",
            len(requests),
            EMBEDDING_MODEL,
        )

        texts = [self._build_text(req) for req in requests]
        logger.debug(
            "EmbeddingClothingService: Textos semánticos construidos: %s",
            texts[:3],  # log solo los primeros 3 para no saturar
        )

        # ── Llamada batch a Gemini ────────────────────────────────────────────
        try:
            result = genai.embed_content(
                model=EMBEDDING_MODEL,
                content=texts,
                task_type=TASK_TYPE,
                output_dimensionality=EXPECTED_DIMENSIONS,
            )
        except ResourceExhausted as e:
            logger.error(
                "EmbeddingClothingService: Cuota de Gemini agotada — %s", e
            )
            raise
        except Unauthenticated as e:
            logger.error(
                "EmbeddingClothingService: API key de Gemini inválida — %s", e
            )
            raise
        except GoogleAPIError as e:
            logger.error(
                "EmbeddingClothingService: Error en la API de Gemini — %s", e
            )
            raise

        # ── Validación de la respuesta ────────────────────────────────────────
        embeddings_raw: list[list[float]] = result["embedding"]

        if len(embeddings_raw) != len(requests):
            raise ValueError(
                f"Gemini devolvió {len(embeddings_raw)} embeddings "
                f"pero se enviaron {len(requests)} prendas. "
                "La respuesta está incompleta."
            )

        # ── Mapeo a schema de respuesta ───────────────────────────────────────
        responses: list[ClothingEmbeddingResponse] = []

        for req, vector in zip(requests, embeddings_raw):
            if len(vector) != EXPECTED_DIMENSIONS:
                raise ValueError(
                    f"Embedding para prenda '{req.clothing_id}' tiene {len(vector)} dims, "
                    f"se esperaban {EXPECTED_DIMENSIONS}. "
                    f"Verifica output_dimensionality en la llamada al modelo."
                )
            responses.append(
                ClothingEmbeddingResponse(
                    clothing_id=req.clothing_id,
                    embedding_vector=vector,
                )
            )
            logger.debug(
                "EmbeddingClothingService: Embedding generado para prenda %s",
                req.clothing_id,
            )

        logger.info(
            "EmbeddingClothingService: %d embeddings generados correctamente.",
            len(responses),
        )
        return responses

    # ── Construcción del texto semántico ──────────────────────────────────────

    def _build_text(self, req: ClothingEmbeddingRequest) -> str:
        """
        Construye el texto semántico que se enviará a Gemini para generar el embedding.

        Plantilla:
            "{category} {style} {color} para {weather} y {occasion}"

        Ejemplos:
            "T-Shirt Casual Navy Blue para Cold y Casual"
            "Blazer Formal Black para All weather y Business Formal"
            "Sneakers Street Wear White para Warm y Sport"

        El orden de los campos importa: category y style definen la identidad
        de la prenda, color aporta el atributo visual más prominente, y
        weather + occasion ubican la prenda en su contexto de uso.
        Esta plantilla fue diseñada para maximizar la separación semántica
        entre prendas en el espacio de embeddings.
        """
        return (
            f"{req.category} {req.style} {req.color} "
            f"para {req.weather} y {req.occasion}"
        )