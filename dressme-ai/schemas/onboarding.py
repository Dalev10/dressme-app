from pydantic import BaseModel, Field, field_validator
from typing import Literal
from uuid import UUID


class EmbeddingItem(BaseModel):
    """
    Representa una tarjeta y su reacción dentro del payload de calibración.

    style_card_id  → UUID de la tarjeta (para trazabilidad en logs)
    embedding      → Los 1536 floats pre-computados de esa tarjeta
    reaction       → Lo que el usuario expresó: LIKE | DISLIKE | SKIP
    """
    style_card_id: UUID
    embedding: list[float] = Field(..., min_length=1536, max_length=1536)
    reaction: Literal["LIKE", "DISLIKE", "SKIP"]

    @field_validator("embedding")
    @classmethod
    def embedding_must_be_1536(cls, v: list[float]) -> list[float]:
        if len(v) != 1536:
            raise ValueError(
                f"El embedding debe tener exactamente 1536 dimensiones, recibió {len(v)}"
            )
        return v


class ComputeTasteVectorRequest(BaseModel):
    """
    Payload que envía dressme-back al endpoint POST /ai/onboarding/compute-taste-vector.

    user_id    → Para logs y trazabilidad. El vector resultante se devuelve
                 y es dressme-back quien lo persiste via PATCH a dressme-database.
    selections → Lista de tarjetas con su embedding y reacción del usuario.
                 Mínimo 1 selección (aunque en la práctica serán 8-12).
    """
    user_id: UUID
    selections: list[EmbeddingItem] = Field(..., min_length=1)



class StyleCardInput(BaseModel):
    """
    Tarjeta a la que se le quiere generar el embedding.

    id                   → UUID que se reenvía en la respuesta para que
                           dressme-back pueda hacer el UPDATE correcto.
    semantic_description → El texto que se manda a OpenAI. Es la descripción
                           semántica rica en keywords de moda que sembramos
                           en data.sql.
    """
    id: UUID
    semantic_description: str = Field(..., min_length=10)


class GenerateEmbeddingsRequest(BaseModel):
    """
    Payload para POST /ai/onboarding/generate-embeddings.
    Recibe todas las tarjetas de una vez para hacer una sola llamada
    batch a OpenAI en lugar de N llamadas individuales.
    """
    style_cards: list[StyleCardInput] = Field(..., min_length=1)


class ComputeTasteVectorResponse(BaseModel):
    """
    Respuesta del cálculo del taste vector.

    user_id         → Echo del user_id recibido (facilita el debug end-to-end)
    taste_vector    → Los 1536 floats del vector resultante normalizado.
                      dressme-back lo toma y hace PATCH /internal/users/{id}
    cards_used      → Cuántas tarjetas contribuyeron al vector
                      (las SKIP tienen peso 0, así que no "contribuyen"
                       pero sí cuentan en el total procesado)
    likes_count     → Señales positivas usadas
    dislikes_count  → Señales negativas usadas
    """
    user_id: UUID
    taste_vector: list[float]
    cards_used: int
    likes_count: int
    dislikes_count: int



class StyleCardEmbeddingResult(BaseModel):
    """Un embedding generado, listo para persistir en tbl_style_cards."""
    id: UUID
    embedding_vector: list[float]


class GenerateEmbeddingsResponse(BaseModel):
    """
    Respuesta del endpoint de generación de embeddings.
    Devuelve los embeddings de todas las tarjetas procesadas.
    """
    embeddings: list[StyleCardEmbeddingResult]
    model_used: str          # Ej: "text-embedding-3-small"
    total_processed: int