"""
schemas/wardrobe.py
────────────────────
Modelos de datos del flujo wardrobe.

Todos los contratos de datos que cruzan capas viven aquí:
  - Lo que entra y sale del router  (VisionAnalysisRequest / Response)
  - Lo que los servicios comparten  (CatalogData)
  - Estructuras internas del dominio (DetectedColorHSL)

"""

from dataclasses import dataclass, field
from decimal import Decimal
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


# ── Catálogo de referencia ────────────────────────────────────────────────────
# Compartido entre CatalogClient (lo produce) y WardrobeAnalysisService
# (lo consume). Ambos lo importan desde aquí.

# ── Config base compartida por todos los modelos del flujo wardrobe ───────────
 
class CamelModel(BaseModel):
    """
    BaseModel con soporte camelCase/snake_case para interop Java ↔ Python.
 
    alias_generator=to_camel   → genera alias camelCase automáticamente
    populate_by_name=True      → acepta tanto el alias (camelCase) como
                                 el nombre Python (snake_case)
    """
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

@dataclass
class CatalogData:
    """
    Catálogo activo de dressme cacheado en memoria.
    Producido por CatalogClient, consumido por WardrobeAnalysisService.
    """
    categories: list[dict]
    styles:     list[dict]
    colors:     list[dict]
    weathers:   list[dict]
    occasions:  list[dict]
    fetched_at: float = field(default_factory=__import__("time").time)

    def is_expired(self, ttl_seconds: int = 600) -> bool:
        import time
        return (time.time() - self.fetched_at) > ttl_seconds


# ── Request / Response del endpoint ──────────────────────────────────────────

class VisionAnalysisRequest(CamelModel):
    """
    Payload que recibe POST /ai/wardrobe/analyze desde dressme-back.
    Acepta tanto snake_case (Python) como camelCase (Java).
    """
    clothing_id: UUID
    image_url:   str = Field(..., description="URL interna accesible desde la red Docker")


class DetectedColorHSL(CamelModel):
    """
    Color dominante de la prenda detectado por Gemini Vision.

    hue/saturation/lightness → valores exactos para el scoring engine
                               (armonía cromática: complementarios, análogos, triádicos)
    color_catalog_id         → UUID del color canónico más cercano del catálogo
                               (para la UI: chip de color, filtros del guardarropa)
    
    Acepta tanto snake_case (Python) como camelCase (Java).
    """
    hue:              int = Field(..., ge=0,   le=360)
    saturation:       int = Field(..., ge=0,   le=100)
    lightness:        int = Field(..., ge=0,   le=100)
    color_catalog_id: UUID


class VisionAnalysisResponse(CamelModel):
    """
    Respuesta de dressme-ai hacia dressme-back.
    dressme-back la descompone en AuditUpdateRequest para persistir en dressme-database.

    M:N real: una prenda puede ser apta para múltiples climas y ocasiones.
    Se envían listas de UUIDs que se persisten en tbl_clothes_weather / tbl_clothes_occasions.

    Serialización JSON: campos aplanados (detected_hue, detected_saturation, detected_lightness).
    """
    clothing_id:            UUID
    predicted_category_id:  UUID
    predicted_style_id:     UUID
    predicted_weather_ids:  list[UUID]
    predicted_occasion_ids: list[UUID]
    detected_color_hsl:     DetectedColorHSL
    confidence_score:       Decimal = Field(..., ge=Decimal("0.00"), le=Decimal("1.00"))
    ai_provider:            str = Field(default="gemini_vision")

    def model_dump_json(self, **kwargs) -> str:
        """
        Serializa a JSON con estructura APLANADA del color para interop con Java.

        Estructura esperada por dressme-back y dressme-database:
        {
          "clothingId": UUID,
          "predictedCategoryId": UUID,
          "predictedStyleId": UUID,
          "predictedColorId": UUID,
          "detectedHue": int,
          "detectedSaturation": int,
          "detectedLightness": int,
          "predictedWeatherIds": [UUID, ...],
          "predictedOccasionIds": [UUID, ...],
          "confidenceScore": float,
          "aiProvider": "gemini_vision"
        }
        """
        import json
        data = {
            "clothingId": str(self.clothing_id),
            "predictedCategoryId": str(self.predicted_category_id),
            "predictedStyleId": str(self.predicted_style_id),
            "predictedColorId": str(self.detected_color_hsl.color_catalog_id),
            "detectedHue": self.detected_color_hsl.hue,
            "detectedSaturation": self.detected_color_hsl.saturation,
            "detectedLightness": self.detected_color_hsl.lightness,
            "predictedWeatherIds": [str(w) for w in self.predicted_weather_ids],
            "predictedOccasionIds": [str(o) for o in self.predicted_occasion_ids],
            "confidenceScore": float(self.confidence_score),
            "aiProvider": self.ai_provider,
        }
        return json.dumps(data)