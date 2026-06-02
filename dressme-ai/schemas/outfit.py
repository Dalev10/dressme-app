"""
schemas/outfit.py
──────────────────
Contratos de datos del flujo de generación de outfits.

Todos los modelos usan CamelModel (heredado de schemas/wardrobe.py) para
garantizar interop camelCase ↔ snake_case con los microservicios Java.

Jerarquía de modelos:
  Entrada al flujo:
    ClothingEmbeddingRequest   → una prenda a vectorizar
    OutfitGenerationRequest    → guardarropa completo + filtros del usuario

  Salida del flujo:
    ClothingEmbeddingResponse  → prenda con su embedding generado
    CandidateSlot              → una prenda asignada a un slot del outfit
    OutfitCandidate            → una combinación completa (TOP+BOTTOM+OUTERWEAR+FOOTWEAR)
    OutfitGenerationResponse   → N candidatos producidos por el generador
"""

from uuid import UUID
from pydantic import Field, field_validator
from schemas.wardrobe import CamelModel


# ── Embedding de una prenda ───────────────────────────────────────────────────

class ClothingEmbeddingRequest(CamelModel):
    """
    Payload para vectorizar una prenda individual.

    El texto semántico que se embede se construye en EmbeddingClothingService
    combinando estos campos:
        "{category} {style} {color} para {weather} y {occasion}"

    clothing_id  → se reenvía en la respuesta para que dressme-back pueda
                   hacer el PATCH correcto en dressme-database.
    """
    clothing_id: UUID
    category:   str = Field(..., min_length=1, description="Nombre de la categoría (ej: T-Shirt)")
    style:      str = Field(..., min_length=1, description="Nombre del estilo (ej: Casual, Boho-Chic)")
    color:      str = Field(..., min_length=1, description="Nombre del color del catálogo (ej: Navy Blue)")
    weather:    str = Field(..., min_length=1, description="Condición climática (ej: Cold, Warm)")
    occasion:   str = Field(..., min_length=1, description="Ocasión (ej: Casual, Formal, Sport)")


class ClothingEmbeddingResponse(CamelModel):
    """
    Respuesta de la vectorización de una prenda.
    dressme-back la persiste via PATCH /internal/wardrobe/{clothingId}/embedding.
    """
    clothing_id:      UUID
    embedding_vector: list[float] = Field(..., min_length=1536, max_length=1536)

    @field_validator("embedding_vector")
    @classmethod
    def must_be_1536_dims(cls, v: list[float]) -> list[float]:
        if len(v) != 1536:
            raise ValueError(
                f"El embedding debe tener exactamente 1536 dimensiones, recibió {len(v)}"
            )
        return v


# ── Candidatos de outfit ──────────────────────────────────────────────────────

class ClothingEmbeddingInfo(CamelModel):
    """
    Prenda del guardarropa con su embedding ya resuelto.
    Es el elemento base que dressme-back envía en OutfitGenerationRequest.

    embedding_vector  → null si la prenda aún no fue vectorizada.
                        El generador ignorará estas prendas con un warning.
    slot              → categoría mapeada al slot lógico del outfit
                        (TOP, BOTTOM, OUTERWEAR, FOOTWEAR, ONEPIECE).
                        Lo resuelve dressme-back antes de llamar a este endpoint.
    hue / saturation / lightness → valores HSL del audit IA, necesarios para
                        que el ColorScoreService evalúe armonía cromática.
    """
    clothing_id:      UUID
    slot:             str = Field(..., description="TOP | BOTTOM | OUTERWEAR | FOOTWEAR | ONEPIECE")
    embedding_vector: list[float] | None = Field(
        default=None,
        description="Vector 1536-dims. None si aún no fue vectorizada."
    )
    hue:        int = Field(..., ge=0, le=360)
    saturation: int = Field(..., ge=0, le=100)
    lightness:  int = Field(..., ge=0, le=100)

    @field_validator("slot")
    @classmethod
    def slot_must_be_valid(cls, v: str) -> str:
        valid = {"TOP", "BOTTOM", "OUTERWEAR", "FOOTWEAR", "ONEPIECE"}
        if v.upper() not in valid:
            raise ValueError(f"slot debe ser uno de {valid}, recibió '{v}'")
        return v.upper()


class CandidateSlot(CamelModel):
    """Una prenda asignada a un slot específico dentro de un outfit candidato."""
    slot:        str  # TOP | BOTTOM | OUTERWEAR | FOOTWEAR | ONEPIECE
    clothing_id: UUID
    hue:        int
    saturation: int
    lightness:  int


class OutfitCandidate(CamelModel):
    """
    Una combinación completa de prendas producida por OutfitGeneratorService.

    slots         → las prendas seleccionadas por slot.
    outfit_vector → promedio de los embedding_vectors de todas las prendas
                    del outfit (1536 dims). dressme-back lo usa para:
                      - TasteScore: similitud coseno con taste_vector del usuario.
                      - DressCode score: similitud coseno con dressCode embedding.
                      - Persistencia en tbl_outfit_ai_audit.outfit_vector.
    """
    slots:         list[CandidateSlot]
    outfit_vector: list[float] = Field(..., min_length=1536, max_length=1536)


# ── Request / Response del endpoint principal ─────────────────────────────────

class OutfitGenerationRequest(CamelModel):
    """
    Payload que recibe POST /ai/outfit/generate desde dressme-back.

    user_id     → para logs y trazabilidad.
    occasion    → nombre de la ocasión filtrada por el usuario (ej: "Casual").
    weather     → nombre del clima filtrado por el usuario (ej: "Cold").
    dress_code  → nombre del dress code opcional (ej: "Smart Casual"). None si no aplica.
    wardrobe    → prendas del usuario ya vectorizadas, enriquecidas con slot y HSL.
                  Solo se incluyen las procesadas (is_processed=true) y
                  con embedding_vector != null.
    """
    user_id:    UUID
    occasion:   str = Field(..., min_length=1)
    weather:    str = Field(..., min_length=1)
    dress_code: str | None = Field(default=None)
    wardrobe:   list[ClothingEmbeddingInfo] = Field(
        ...,
        min_length=1,
        description="Prendas del guardarropa con embedding y slot resueltos."
    )


class OutfitGenerationResponse(CamelModel):
    """
    Respuesta de POST /ai/outfit/generate.

    candidates       → lista de outfits candidatos listos para scoring.
    total_candidates → número de combinaciones generadas (puede diferir de
                       len(candidates) si se aplicó un límite de N).
    skipped_clothing → número de prendas ignoradas por falta de embedding o slot inválido.
    """
    candidates:       list[OutfitCandidate]
    total_candidates: int
    skipped_clothing: int = Field(default=0)