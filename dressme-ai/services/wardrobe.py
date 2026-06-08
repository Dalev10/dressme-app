"""
services/wardrobe_service.py
"""

import base64
import json
import logging
import re
from decimal import Decimal
from uuid import UUID

import requests
from google import genai
from google.genai import types

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
    def __init__(self, api_key: str, catalog: CatalogProvider):
        self._client = genai.Client(api_key=api_key)
        self._catalog = catalog
        logger.info("WardrobeAnalysisService: Inicializado con modelo %s", GEMINI_VISION_MODEL)

    def analyze(self, request: VisionAnalysisRequest) -> VisionAnalysisResponse:
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

    def _call_gemini(self, image_url: str, catalog: CatalogData) -> str:
        logger.info("WardrobeAnalysisService: Descargando imagen desde %s", image_url)
        response = requests.get(image_url, timeout=30)
        response.raise_for_status()

        mime_type = "image/jpeg"
        if image_url.lower().endswith(".png"):
            mime_type = "image/png"
        elif image_url.lower().endswith(".webp"):
            mime_type = "image/webp"
        elif image_url.lower().endswith(".gif"):
            mime_type = "image/gif"

        image_base64 = base64.standard_b64encode(response.content).decode("utf-8")
        logger.info(
            "WardrobeAnalysisService: Imagen descargada (%d bytes, tipo: %s)",
            len(response.content), mime_type
        )

        gemini_response = self._client.models.generate_content(
            model=GEMINI_VISION_MODEL,
            contents=[
                types.Part.from_text(text=self._build_prompt(catalog)),
                types.Part.from_bytes(data=base64.b64decode(image_base64), mime_type=mime_type),
            ],
        )

        return gemini_response.text

    def _build_prompt(self, catalog: CatalogData) -> str:
        def fmt(entries: list[dict]) -> str:
            return "\n".join(
                f'  {{"id": "{e["id"]}", "name": "{e["name"]}"}}'
                for e in entries
            )

        return f"""You are a strict fashion classification engine. Your job is to analyze garments with maximum precision and diversity. Lazy or repetitive classifications are WRONG.

CRITICAL RULES — VIOLATIONS WILL BREAK THE SYSTEM:
1. Identify ONLY the main clothing item. Ignore backgrounds, models, mannequins, accessories, and packaging.
2. You MUST use ONLY the exact "id" values from the catalogs below. NEVER invent or modify IDs.
3. Respond ONLY with the JSON object. No markdown, no explanation, no preamble.
4. DIVERSITY IS MANDATORY — read the anti-bias rules below before classifying.

ANTI-BIAS RULES — READ BEFORE SELECTING ANYTHING:
- STYLE & OCCASION MAPPING:
You have {catalog.styles} styles and {catalog.occasions} occasions available.
Your task is to assign ALL styles that genuinely fit the garment (not just one),
and ALL occasions where it could realistically be worn.

═══════════════════════════════════════════════════════════════
STYLE DEFINITIONS AND THEIR VALID OCCASIONS
═══════════════════════════════════════════════════════════════

[Pure Minimalist]
  Signature: Solid neutrals (white, black, beige, grey), zero logos, clean silhouettes, quality basics.
  Valid occasions: Everyday, Work, Date Night, Travel, Party
  NEVER assign to: Sport, Beach & Pool, Festival

[Quiet Luxury]
  Signature: Cashmere, silk, fine wool, tailored cuts, muted tones, no visible branding.
  Valid occasions: Work, Formal Event, Date Night, Party, Travel
  NEVER assign to: Sport, Beach & Pool, Festival, Outdoor

[Normcore]
  Signature: Generic basics (white tee, khakis, plain sneakers), deliberately average, anti-fashion.
  Valid occasions: Everyday, Travel, Outdoor
  NEVER assign to: Formal Event, Party, Work, Date Night

[Skater]
  Signature: Baggy cargo pants, skate brand hoodies, canvas shoes (Vans/DC), graphic tees.
  Valid occasions: Everyday, Festival, Outdoor, Travel
  NEVER assign to: Formal Event, Work, Date Night

[Techwear]
  Signature: Utility pockets, waterproof/synthetic fabrics, straps, modular layering, monochromatic palette.
  Valid occasions: Everyday, Outdoor, Travel, Festival
  NEVER assign to: Formal Event, Beach & Pool, Work

[Hypebeast]
  Signature: Supreme/Off-White/Jordan drops, oversized fits, loud colorblocking, visible luxury logos.
  Valid occasions: Everyday, Party, Festival, Travel
  NEVER assign to: Formal Event, Work, Sport, Beach & Pool

[Corporate Formal]
  Signature: Full suit with tie, dress shirt, Oxford shoes, structured briefcase silhouette.
  Valid occasions: Work, Formal Event
  NEVER assign to: Everyday, Sport, Festival, Beach & Pool, Outdoor, Travel

[Old Money]
  Signature: Polo shirts, boat shoes, blazers, Oxford shirts, plaid patterns, heritage brands (Barbour, Ralph Lauren).
  Valid occasions: Work, Formal Event, Date Night, Party, Travel, Everyday
  NEVER assign to: Sport, Festival, Beach & Pool

[Boho-Chic]
  Signature: Flowy maxi dresses/skirts, earthy prints, suede fringe, layered jewelry, wide-brim hats.
  Valid occasions: Everyday, Party, Festival, Travel, Date Night, Beach & Pool
  NEVER assign to: Work, Formal Event, Sport

[Festival Folk]
  Signature: Crochet tops, fringe jackets, western boots, embroidered denim, indie accessories.
  Valid occasions: Festival, Party, Everyday, Outdoor, Travel
  NEVER assign to: Work, Formal Event, Sport, Beach & Pool

[Gym Athletic]
  Signature: Compression leggings, seamless sports bras, dry-fit tees, training shoes, performance fabrics.
  Valid occasions: Sport, Everyday, Outdoor, Travel
  NEVER assign to: Formal Event, Work, Date Night, Party

[Gothic Scholarly]
  Signature: Dark wool coats, tweed blazers, turtlenecks, dark academia color palette (black, burgundy, forest green).
  Valid occasions: Everyday, Work, Date Night, Party
  NEVER assign to: Sport, Festival, Beach & Pool, Outdoor

[Preppy Classic]
  Signature: Argyle sweaters, pleated skirts, loafers, polos, headbands, blazers with crests.
  Valid occasions: Work, Everyday, Date Night, Party, Travel
  NEVER assign to: Sport, Festival, Beach & Pool

[Y2K Pop]
  Signature: Low-rise jeans, butterfly clips, metallic fabrics, baby tees, platform shoes, candy colors.
  Valid occasions: Party, Festival, Everyday, Date Night
  NEVER assign to: Work, Formal Event, Sport, Outdoor

[90s Grunge]
  Signature: Flannel shirts (oversized), distressed denim, combat boots, band tees, fishnet layers.
  Valid occasions: Everyday, Party, Festival, Outdoor
  NEVER assign to: Work, Formal Event, Date Night, Sport

[Prairie Romantic]
  Signature: Puffed sleeves, floral linen/cotton, lace trim, cottagecore silhouettes, mary jane shoes.
  Valid occasions: Everyday, Date Night, Party, Travel, Festival, Outdoor
  NEVER assign to: Work, Sport, Formal Event

[Smart Corporate]
  Signature: Unstructured blazers, chinos, turtlenecks, loafers or clean derbies — business casual without a tie.
  Valid occasions: Work, Date Night, Party, Travel, Everyday
  NEVER assign to: Sport, Festival, Beach & Pool

[Linen Breeze]
  Signature: Linen shirts/pants, nautical stripes, espadrilles, resort-wear silhouettes, breathable fabrics.
  Valid occasions: Everyday, Travel, Beach & Pool, Party, Outdoor, Date Night
  NEVER assign to: Formal Event, Work, Sport

[Punk Rock]
  Signature: Studded leather jackets, ripped black denim, band tees, safety pins, Dr. Martens.
  Valid occasions: Everyday, Party, Festival, Outdoor
  NEVER assign to: Work, Formal Event, Sport, Beach & Pool

[Cyberpunk / Dark Avant-Garde]
  Signature: Asymmetrical silhouettes, PVC/latex textures, black holographic elements, deconstructed tailoring.
  Valid occasions: Party, Festival, Date Night
  NEVER assign to: Work, Formal Event, Sport, Outdoor, Everyday, Travel, Beach & Pool

[Everyday Polished]
  Signature: Fitted jeans, clean crewnecks, white sneakers, leather shoes — neat casual with zero effort showing.
  Valid occasions: Everyday, Travel, Work (casual office), Date Night, Party, Outdoor
  NEVER assign to: Formal Event, Sport, Beach & Pool, Festival

═══════════════════════════════════════════════════════════════
ASSIGNMENT RULES
═══════════════════════════════════════════════════════════════

1. MULTI-ASSIGN MANDATORY: A single garment MUST receive multiple styles and multiple occasions
   if its physical attributes genuinely match them. Never collapse to one.

2. GARMENT DRIVES THE DECISION: Base assignment entirely on fabric, silhouette, construction,
   and visible details — not on what the user said it's "for."

3. STYLE ELIMINATION FIRST: Before selecting, rule out all styles whose signature
   clearly contradicts the garment. Assign only from what remains.

4. OCCASION UNION: The final occasion list = union of valid occasions from ALL assigned styles,
   filtered by what makes physical sense for the garment.

5. HARD BOUNDARIES OVERRIDE EVERYTHING:
   — A blazer, dress shirt, trench coat, or tailored trouser is NEVER Normcore, Skater, or Gym Athletic.
   — A leather jacket is NEVER Corporate Formal or Quiet Luxury.
   — Activewear (compression, dry-fit) is NEVER Formal Event or Work.
   — A swim trunk or bikini is NEVER Work, Formal Event, or Date Night.
   
- WEATHER: Select ALL weather conditions this garment is genuinely suitable for. A lightweight cotton shirt fits Hot AND Mild. A wool coat fits Cold AND Mild. You MUST return at least 2 weather IDs unless the garment is extreme (heavy parka → only Cold is correct).
- OCCASIONS: "Everyday" is ONLY for genuinely casual, unremarkable clothing worn daily (basic t-shirts, simple jeans, casual sneakers). Any garment with structure, formality, or specific function belongs to a different occasion:
  · Blazer, dress shirt, trousers → Work, Formal Event
  · Athletic wear, shorts, running shoes → Sport
  · Cocktail dress, heels, suit → Formal Event, Party, Date Night
  · Swimwear, sandals → Beach & Pool
  · Hiking boots, cargo pants → Outdoor
  · Sequins, bold prints → Party, Festival
  If the garment could plausibly be worn to a specific event, include that event.
  Returning only "Everyday" for any structured or purposeful garment is WRONG.
- PENALIZATION: If you return "Everyday" for style on any garment that is clearly not casual, your classification is wrong. If you return only 1 weather or 1 occasion without strong justification, your classification is incomplete.

STEP-BY-STEP CLASSIFICATION (reason before outputting):
Step 1: What is the exact garment type? (blazer, hoodie, dress, etc.)
Step 2: What is the dominant color? Convert to HSL.
Step 3: Eliminate obviously wrong styles. Which 3+ styles clearly don't fit?
Step 4: Which style fits BEST from the remaining options?
Step 5: Which weather conditions genuinely apply? List all valid ones.
Step 6: Which occasions genuinely apply? List all valid ones.
Step 7: Output the JSON.

CATALOG — CATEGORIES:
{fmt(catalog.categories)}

CATALOG — STYLES:
{fmt(catalog.styles)}

CATALOG — COLORS (choose the closest match to the detected color):
{fmt(catalog.colors)}

CATALOG — WEATHER (ALL suitable conditions — minimum 2 unless extreme garment):
{fmt(catalog.weathers)}

CATALOG — OCCASIONS (ALL suitable occasions — minimum 2):
{fmt(catalog.occasions)}

NEGATIVE EXAMPLE — DO NOT DO THIS:
{{"category_id": "...", "style_id": "<everyday_id>", "weather_ids": ["<mild_id>"], "occasion_ids": ["<everyday_id>"]}}
Reason this is wrong: Everyday style on a non-casual item, only 1 weather, only 1 occasion.

REQUIRED JSON FORMAT — output exactly this structure:
{{
  "category_id":   "<uuid from CATEGORIES>",
  "style_id":      "<uuid from STYLES>",
  "weather_ids":   ["<uuid>", "<uuid>"],
  "occasion_ids":  ["<uuid>", "<uuid>"],
  "color": {{
    "catalog_id": "<uuid from COLORS>",
    "hue":        <integer 0-360>,
    "saturation": <integer 0-100>,
    "lightness":  <integer 0-100>
  }},
  "confidence": <float 0.0-1.0>
}}"""

    def _parse_response(self, raw_text: str) -> dict:
        cleaned = re.sub(r"```(?:json)?", "", raw_text).strip()
        try:
            data = json.loads(cleaned)
        except json.JSONDecodeError as e:
            raise ValueError(f"Gemini no devolvió JSON válido. Raw: {raw_text!r}. Error: {e}") from e

        required_root  = {"category_id", "style_id", "weather_ids", "occasion_ids", "color", "confidence"}
        required_color = {"catalog_id", "hue", "saturation", "lightness"}

        if missing := required_root - set(data.keys()):
            raise ValueError(f"Gemini omitió claves: {missing}")
        if missing_color := required_color - set(data["color"].keys()):
            raise ValueError(f"Gemini omitió campos del color: {missing_color}")

        for key in ("category_id", "style_id"):
            try:
                UUID(str(data[key]))
            except ValueError:
                raise ValueError(f"'{data[key]}' para '{key}' no es un UUID válido.")

        for list_key in ("weather_ids", "occasion_ids"):
            if not isinstance(data[list_key], list) or len(data[list_key]) == 0:
                raise ValueError(f"'{list_key}' debe ser una lista no vacía.")
            for uid in data[list_key]:
                try:
                    UUID(str(uid))
                except ValueError:
                    raise ValueError(f"'{uid}' en '{list_key}' no es un UUID válido.")

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

    def _build_response(self, clothing_id: UUID, data: dict) -> VisionAnalysisResponse:
        c = data["color"]
        return VisionAnalysisResponse(
            clothing_id=clothing_id,
            predicted_category_id=UUID(data["category_id"]),
            predicted_style_id=UUID(data["style_id"]),
            predicted_weather_ids=[UUID(str(w)) for w in data["weather_ids"]],
            predicted_occasion_ids=[UUID(str(o)) for o in data["occasion_ids"]],
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
            predicted_weather_ids=[first(catalog.weathers)],
            predicted_occasion_ids=[first(catalog.occasions)],
            detected_color_hsl=DetectedColorHSL(
                hue=0, saturation=0, lightness=50,
                color_catalog_id=first(catalog.colors),
            ),
            confidence_score=Decimal("0.0"),
            ai_provider="gemini_vision",
        )
