"""
services/wardrobe_service.py
─────────────────────────────
Orquestador del análisis de prendas usando Gemini Vision.

Importaciones:
  - schemas.wardrobe  → CatalogData, VisionAnalysisRequest/Response, DetectedColorHSL
"""

import base64
import json
import logging
import re
from decimal import Decimal
from pathlib import Path
from uuid import UUID

import google.generativeai as genai
import requests

from schemas.wardrobe import (
    CatalogData,
    VisionAnalysisRequest,
    VisionAnalysisResponse,
    DetectedColorHSL,
)
from services.catalog_provider import CatalogProvider

logger = logging.getLogger(__name__)

GEMINI_VISION_MODEL = "gemini-2.5-flash"


class WardrobeAnalysisService:
    """
    Analiza imágenes de prendas con Gemini Vision y mapea el resultado
    al catálogo de dressme en una sola llamada a la API.

    Constructor:
      api_key  → str,             inyectado desde settings via dependencies.py
      catalog  → CatalogProvider, inyectado via dependencies.py
                 (WardrobeAnalysisService no sabe qué implementación es)
    """

    def __init__(self, api_key: str, catalog: CatalogProvider):
        genai.configure(api_key=api_key)
        self._model   = genai.GenerativeModel(model_name=GEMINI_VISION_MODEL)
        self._catalog = catalog
        logger.info("WardrobeAnalysisService: Inicializado con modelo %s", GEMINI_VISION_MODEL)

    def analyze(self, request: VisionAnalysisRequest) -> VisionAnalysisResponse:
        """
        Analiza la prenda y devuelve su clasificación completa.

        Fallback: si Gemini falla, la prenda se asigna a "Uncategorized"
        con confidence_score=0.0. El usuario puede corregirla manualmente.
        """
        logger.info(
            "WardrobeAnalysisService: Analizando prenda %s — url=%s",
            request.clothing_id, request.image_url,
        )
        catalog = self._catalog.get_catalog()

        try:
            raw  = self._call_gemini(request.image_url, catalog)
            data = self._parse_response(raw)
            return self._build_response(request.clothing_id, data)
        except Exception as e:
            logger.error(
                "WardrobeAnalysisService: Error analizando prenda %s: %s. "
                "Aplicando fallback a 'Uncategorized'.",
                request.clothing_id, e,
            )
            return self._build_uncategorized_response(request.clothing_id, catalog)

    # ── Gemini Vision ─────────────────────────────────────────────────────────

    def _call_gemini(self, image_url: str, catalog: CatalogData) -> str:
        """
        Analiza la imagen usando Gemini Vision con base64 inline data.
        
        Proceso:
          1. Descargar la imagen desde la URL (interna a la red Docker)
          2. Convertir a base64
          3. Detectar el tipo MIME
          4. Llamar a Gemini con inlineData
        """
        logger.info("WardrobeAnalysisService: Descargando imagen desde %s", image_url)
        response = requests.get(image_url, timeout=30)
        response.raise_for_status()
        
        # Detectar el tipo MIME basado en la extensión
        mime_type = "image/jpeg"  # por defecto
        if image_url.lower().endswith(".png"):
            mime_type = "image/png"
        elif image_url.lower().endswith(".webp"):
            mime_type = "image/webp"
        elif image_url.lower().endswith(".gif"):
            mime_type = "image/gif"
        
        # Convertir la imagen a base64
        image_base64 = base64.standard_b64encode(response.content).decode("utf-8")
        logger.info(
            "WardrobeAnalysisService: Imagen descargada y convertida a base64 (%d bytes, tipo: %s)",
            len(response.content), mime_type
        )
        
        # Llamar a Gemini con inlineData
        gemini_response = self._model.generate_content(
            contents=[{
                "parts": [
                    {"text": self._build_prompt(catalog)},
                    {"inline_data": {
                        "mime_type": mime_type,
                        "data": image_base64
                    }},
                ]
            }]
        )
        
        return gemini_response.text

    def _build_prompt(self, catalog: CatalogData) -> str:
        def fmt(entries: list[dict]) -> str:
            return "\n".join(
                f'  {{"id": "{e["id"]}", "name": "{e["name"]}"}}'
                for e in entries
            )

        return f"""You are a professional fashion analyst for a clothing recommendation app.

Analyze the clothing item visible in the image and return a JSON classification.

INSTRUCTIONS:
1. Identify the main clothing item (ignore backgrounds, models, accessories).
2. Detect the dominant color of the garment and convert it to HSL values.
3. Select the best matching entry from EACH catalog list below.
4. You MUST use the exact "id" values from the lists — never invent IDs.
5. Respond ONLY with the JSON object. No markdown, no explanation.

CATALOG — CATEGORIES:
{fmt(catalog.categories)}

CATALOG — STYLES:
{fmt(catalog.styles)}

CATALOG — COLORS (choose the closest match to the detected color):
{fmt(catalog.colors)}

CATALOG — WEATHER (most suitable condition for this garment):
{fmt(catalog.weathers)}

CATALOG — OCCASIONS (most suitable occasion):
{fmt(catalog.occasions)}

REQUIRED JSON FORMAT:
{{
  "category_id":  "<uuid from CATEGORIES>",
  "style_id":     "<uuid from STYLES>",
  "weather_id":   "<uuid from WEATHER>",
  "occasion_id":  "<uuid from OCCASIONS>",
  "color": {{
    "catalog_id": "<uuid from COLORS>",
    "hue":        <integer 0-360>,
    "saturation": <integer 0-100>,
    "lightness":  <integer 0-100>
  }},
  "confidence": <float 0.0-1.0>
}}"""

    # ── Parseo ────────────────────────────────────────────────────────────────

    def _parse_response(self, raw_text: str) -> dict:
        cleaned = re.sub(r"```(?:json)?", "", raw_text).strip()
        try:
            data = json.loads(cleaned)
        except json.JSONDecodeError as e:
            raise ValueError(f"Gemini no devolvió JSON válido. Raw: {raw_text!r}. Error: {e}") from e

        required_root  = {"category_id", "style_id", "weather_id", "occasion_id", "color", "confidence"}
        required_color = {"catalog_id", "hue", "saturation", "lightness"}

        if missing := required_root - set(data.keys()):
            raise ValueError(f"Gemini omitió claves: {missing}")
        if missing_color := required_color - set(data["color"].keys()):
            raise ValueError(f"Gemini omitió campos del color: {missing_color}")

        for key in ("category_id", "style_id", "weather_id", "occasion_id"):
            try:    
                UUID(str(data[key]))
            except ValueError:
                raise ValueError(f"'{data[key]}' para '{key}' no es un UUID válido.")

        try:    
            UUID(str(data["color"]["catalog_id"]))
        except ValueError:
            raise ValueError(f"'{data['color']['catalog_id']}' no es UUID válido para color.catalog_id.")

        c = data["color"]
        if not (0 <= int(c["hue"])        <= 360): 
            raise ValueError(f"Hue fuera de rango: {c['hue']}")
        if not (0 <= int(c["saturation"]) <= 100): 
            raise ValueError(f"Saturation fuera de rango: {c['saturation']}")
        if not (0 <= int(c["lightness"])  <= 100): 
            raise ValueError(f"Lightness fuera de rango: {c['lightness']}")

        return data

    # ── Builders ──────────────────────────────────────────────────────────────

    def _build_response(self, clothing_id: UUID, data: dict) -> VisionAnalysisResponse:
        c = data["color"]
        return VisionAnalysisResponse(
            clothing_id=clothing_id,
            predicted_category_id=UUID(data["category_id"]),
            predicted_style_id=UUID(data["style_id"]),
            predicted_weather_id=UUID(data["weather_id"]),
            predicted_occasion_id=UUID(data["occasion_id"]),
            detected_color_hsl=DetectedColorHSL(
                hue=int(c["hue"]),
                saturation=int(c["saturation"]),
                lightness=int(c["lightness"]),
                color_catalog_id=UUID(c["catalog_id"]),
            ),
            confidence_score=Decimal(str(round(float(data["confidence"]), 4))),
            ai_provider="gemini_vision",
        )

    def _build_uncategorized_response(
        self, clothing_id: UUID, catalog: CatalogData
    ) -> VisionAnalysisResponse:
        def first(entries: list[dict]) -> UUID:
            if not entries:
                raise RuntimeError("Catálogo vacío. Verifica el seed de dressme-database.")
            return UUID(entries[0]["id"])

        uncategorized = next(
            (c for c in catalog.categories if c["name"].lower() == "uncategorized"), None
        )
        return VisionAnalysisResponse(
            clothing_id=clothing_id,
            predicted_category_id=UUID(uncategorized["id"]) if uncategorized else first(catalog.categories),
            predicted_style_id=first(catalog.styles),
            predicted_weather_id=first(catalog.weathers),
            predicted_occasion_id=first(catalog.occasions),
            detected_color_hsl=DetectedColorHSL(
                hue=0, saturation=0, lightness=50,
                color_catalog_id=first(catalog.colors),
            ),
            confidence_score=Decimal("0.0"),
            ai_provider="gemini_vision",
        )