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

        return f"""You are a strict fashion classification engine. Your job is to analyze garments with
maximum precision and diversity. Lazy, collapsed, or repetitive classifications are WRONG.

CRITICAL RULES — VIOLATIONS WILL BREAK THE SYSTEM:
1. Identify ONLY the main clothing item. Ignore backgrounds, models, mannequins, accessories, and packaging.
2. You MUST use ONLY the exact "id" values from the catalogs below. NEVER invent or modify IDs.
3. Respond ONLY with the JSON object. No markdown, no explanation, no preamble.
4. DIVERSITY IS MANDATORY — read ALL mapping rules below before classifying anything.

═══════════════════════════════════════════════════════════════════════
PART 1 — STYLE & OCCASION MAPPING
═══════════════════════════════════════════════════════════════════════

Your task is to assign ALL micro-styles that genuinely fit the garment (not just one),
then derive the occasion list as the union of valid occasions from every assigned style,
filtered by what the garment's physical attributes actually support.

── STYLE DEFINITIONS, SIGNATURES, AND VALID OCCASIONS ──────────────────

[Pure Minimalist]
  Signature: Solid neutrals (white, black, beige, grey), zero logos, clean silhouettes, quality basics.
  Valid occasions: Everyday, Work, Date Night, Travel, Party
  NEVER assign to: Sport, Beach & Pool, Festival

[Quiet Luxury]
  Signature: Cashmere, silk, fine wool, tailored cuts, muted tones, no visible branding.
  Valid occasions: Work, Formal Event, Date Night, Party, Travel
  NEVER assign to: Sport, Beach & Pool, Festival, Outdoor

[Normcore]
  Signature: Generic basics (plain white tee, khakis, plain sneakers), deliberately average, anti-fashion.
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
  Signature: Full suit with tie, dress shirt, Oxford shoes, rigid structured tailoring.
  Valid occasions: Work, Formal Event
  NEVER assign to: Everyday, Sport, Festival, Beach & Pool, Outdoor, Travel

[Old Money]
  Signature: Polo shirts, boat shoes, blazers, Oxford shirts, plaid/herringbone, heritage brands (Barbour, Ralph Lauren).
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
  Signature: Dark wool coats, tweed blazers, turtlenecks, dark academia palette (black, burgundy, forest green).
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
  Signature: Puffed sleeves, floral linen/cotton, lace trim, cottagecore silhouettes, Mary Jane shoes.
  Valid occasions: Everyday, Date Night, Party, Travel, Festival, Outdoor
  NEVER assign to: Work, Sport, Formal Event

[Smart Corporate]
  Signature: Unstructured blazers, chinos, turtlenecks, loafers or clean derbies — business casual without a tie.
  Valid occasions: Work, Date Night, Party, Travel, Everyday
  NEVER assign to: Sport, Festival, Beach & Pool

[Linen Breeze]
  Signature: Linen shirts/pants, nautical stripes, espadrilles, resort-wear silhouettes, breathable natural fabrics.
  Valid occasions: Everyday, Travel, Beach & Pool, Party, Outdoor, Date Night
  NEVER assign to: Formal Event, Work, Sport

[Punk Rock]
  Signature: Studded leather jackets, ripped black denim, band tees, safety pins, Dr. Martens.
  Valid occasions: Everyday, Party, Festival, Outdoor
  NEVER assign to: Work, Formal Event, Sport, Beach & Pool

[Cyberpunk / Dark Avant-Garde]
  Signature: Asymmetrical silhouettes, PVC/latex textures, holographic black elements, deconstructed tailoring.
  Valid occasions: Party, Festival, Date Night
  NEVER assign to: Work, Formal Event, Sport, Outdoor, Everyday, Travel, Beach & Pool

[Everyday Polished]
  Signature: Fitted jeans, clean crewnecks, white sneakers, leather shoes — neat casual with zero effort showing.
  Valid occasions: Everyday, Travel, Work (casual office), Date Night, Party, Outdoor
  NEVER assign to: Formal Event, Sport, Beach & Pool, Festival

── STYLE ASSIGNMENT RULES ────────────────────────────────────────────────

1. MULTI-ASSIGN MANDATORY: A single garment MUST receive multiple styles if its physical
   attributes match them. Never collapse to one style.

2. GARMENT DRIVES THE DECISION: Base assignment entirely on fabric, silhouette, construction,
   and visible details — not on what the user said it's "for."

3. ELIMINATION FIRST: Before selecting, rule out every style whose signature clearly
   contradicts the garment. Assign only from what survives elimination.

4. OCCASION UNION: The final occasion list = union of valid occasions from ALL assigned styles,
   filtered by what makes physical sense for the garment.

5. HARD STYLE BOUNDARIES — OVERRIDE EVERYTHING:
   — A blazer, dress shirt, trench coat, or tailored trouser is NEVER Normcore, Skater, or Gym Athletic.
   — A leather jacket is NEVER Corporate Formal or Quiet Luxury.
   — Activewear (compression, dry-fit) is NEVER Formal Event or Work.
   — A swim trunk or bikini is NEVER Work, Formal Event, or Date Night.


═══════════════════════════════════════════════════════════════════════
PART 2 — WEATHER MAPPING BY GARMENT CATEGORY
═══════════════════════════════════════════════════════════════════════

Weather IDs reference:
  Hot      → 77777777-0000-0000-0000-000000000001  (above 28 °C, lightweight fabrics)
  Warm     → 77777777-0000-0000-0000-000000000002  (20–28 °C, light layers)
  Mild     → 77777777-0000-0000-0000-000000000003  (12–20 °C, light jacket / long sleeves)
  Cold     → 77777777-0000-0000-0000-000000000004  (below 12 °C, warm layers)
  Rainy    → 77777777-0000-0000-0000-000000000005  (wet conditions)
  Freezing → 77777777-0000-0000-0000-000000000006  (below 0 °C, heavy winter gear)
  Windy    → 77777777-0000-0000-0000-000000000007  (strong wind, structured outerwear)

── WEATHER BY CATEGORY ──────────────────────────────────────────────────

T-Shirt (55555555-…-000000000011):
  Fabric-light (cotton, linen, jersey): Hot, Warm
  Heavyweight or long-sleeve: Warm, Mild
  NEVER: Cold, Freezing, Rainy alone (always pair with Warm or Hot)

Shirt (55555555-…-000000000012):
  Linen / chambray / poplin: Hot, Warm
  Oxford / flannel / twill: Warm, Mild, Cold
  Waterproof / treated: Rainy, Mild
  NEVER assign only Mild if the fabric is clearly summer-weight

Blouse (55555555-…-000000000013):
  Sheer / silk / chiffon: Hot, Warm
  Crepe / satin: Warm, Mild
  NEVER: Cold, Freezing

Sweater (55555555-…-000000000014):
  Fine knit: Mild, Cold
  Chunky / cable knit / cashmere: Cold, Freezing
  NEVER: Hot alone

Hoodie (55555555-…-000000000015):
  Fleece-lined: Cold, Mild, Rainy
  Lightweight: Warm, Mild
  NEVER: Hot, Freezing

Tank Top (55555555-…-000000000016):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy, Windy

Crop Top (55555555-…-000000000017):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy, Windy

Jeans (55555555-…-000000000021):
  Lightweight denim: Warm, Mild
  Standard denim: Mild, Cold, Rainy
  Heavyweight / lined: Cold, Freezing
  MINIMUM: 2 weather IDs always

Trousers (55555555-…-000000000022):
  Linen / cotton / chino: Hot, Warm, Mild
  Wool / flannel: Mild, Cold
  NEVER assign only 1 weather ID

Shorts (55555555-…-000000000023):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy alone

Skirt (55555555-…-000000000024):
  Mini / cotton: Hot, Warm
  Midi / maxi: Warm, Mild
  Wool / layered: Mild, Cold
  NEVER: Freezing alone

Leggings (55555555-…-000000000025):
  Athletic / thin: Warm, Mild, Sport context
  Thermal: Cold, Freezing
  NEVER assign only 1 weather ID

Joggers (55555555-…-000000000026):
  Lightweight: Warm, Mild
  Fleece: Cold, Mild
  NEVER: Hot, Freezing

Jacket (55555555-…-000000000031):
  Denim / lightweight bomber: Mild, Warm
  Leather (standard): Cold, Mild, Windy
  Windbreaker: Mild, Rainy, Windy
  Puffer (light): Cold, Mild
  MINIMUM: 2 weather IDs always

Coat (55555555-…-000000000032):
  Trench: Mild, Rainy, Windy
  Wool overcoat: Cold, Mild, Windy
  Puffer / down: Cold, Freezing, Rainy
  NEVER: Hot, Warm

Blazer (55555555-…-000000000033):
  Unstructured linen: Warm, Mild, Hot
  Wool / structured: Mild, Cold
  NEVER: Freezing alone

Vest (55555555-…-000000000034):
  Puffer vest: Cold, Mild, Windy
  Suit vest: Mild, Cold
  NEVER: Hot, Warm

Dress (55555555-…-000000000041):
  Mini / sundress / cotton: Hot, Warm
  Midi / satin / cocktail: Warm, Mild
  Maxi / wool: Mild, Cold
  NEVER: Freezing alone

Jumpsuit (55555555-…-000000000042):
  Linen / cotton: Hot, Warm
  Structured / tailored: Warm, Mild
  NEVER: Cold, Freezing

Romper (55555555-…-000000000043):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy

Sneakers (55555555-…-000000000051):
  Canvas / mesh: Warm, Mild, Hot
  Leather / high-top: Mild, Cold, Rainy
  NEVER: Freezing alone

Boots (55555555-…-000000000052):
  Ankle / leather: Cold, Mild, Rainy, Windy
  Combat: Cold, Mild, Rainy
  Over-the-knee: Cold, Freezing
  NEVER: Hot, Warm

Loafers & Oxfords (55555555-…-000000000053):
  Always: Mild, Warm, Cold
  NEVER: Hot, Freezing alone

Sandals (55555555-…-000000000054):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy, Windy

Heels (55555555-…-000000000055):
  Open-toe: Warm, Mild
  Closed-toe: Mild, Cold
  NEVER: Hot, Rainy, Freezing

Sports Bra (55555555-…-000000000071):
  Always: Hot, Warm (sport context overrides)
  NEVER: Cold, Freezing

Athletic Shorts (55555555-…-000000000072):
  Always: Hot, Warm
  NEVER: Cold, Freezing, Rainy

Track Jacket (55555555-…-000000000073):
  Always: Mild, Warm, Rainy (light wind protection)
  NEVER: Hot, Freezing

── WEATHER ASSIGNMENT RULES ──────────────────────────────────────────────

W1. MINIMUM 2 WEATHER IDs: Only extreme garments justify a single weather
    (e.g., a heavy parka = Cold + Freezing only; a bikini = Hot only).
    All other garments MUST return at least 2 weather IDs.

W2. FABRIC DECIDES, NOT CATEGORY: A linen blazer and a wool blazer have
    completely different weather profiles. Inspect the fabric first.

W3. RAINY AND WINDY ARE ADDITIVE: They do not replace Mild/Cold. A trench
    coat is Mild + Rainy + Windy, not just Rainy.

W4. NEVER ASSIGN HOT + COLD OR HOT + FREEZING to the same garment.
    These are mutually exclusive ranges.


═══════════════════════════════════════════════════════════════════════
PART 3 — OCCASION MAPPING BY GARMENT CATEGORY
═══════════════════════════════════════════════════════════════════════

Occasion IDs reference:
  Everyday     → 44444444-0000-0000-0000-000000000001
  Work         → 44444444-0000-0000-0000-000000000002
  Party        → 44444444-0000-0000-0000-000000000003
  Sport        → 44444444-0000-0000-0000-000000000004
  Formal Event → 44444444-0000-0000-0000-000000000005
  Outdoor      → 44444444-0000-0000-0000-000000000006
  Date Night   → 44444444-0000-0000-0000-000000000007
  Travel       → 44444444-0000-0000-0000-000000000008
  Beach & Pool → 44444444-0000-0000-0000-000000000009
  Festival     → 44444444-0000-0000-0000-000000000010

── OCCASION MAPPING TABLE ────────────────────────────────────────────────

T-Shirt:
  Plain / solid / no print: Everyday, Travel, Outdoor, Sport (if moisture-wicking)
  Graphic / band / vintage: Everyday, Festival, Party, Outdoor, Travel
  Oversized / boxy: Everyday, Festival, Travel, Outdoor
  NEVER: Formal Event, Work (unless clearly business-casual branded)

Shirt (button-down):
  Linen / casual check: Everyday, Travel, Outdoor, Date Night, Party
  Oxford / poplin (untucked): Everyday, Work, Travel, Date Night
  Dress shirt / French-cuff: Work, Formal Event, Date Night, Party
  Flannel (oversized): Everyday, Outdoor, Festival
  NEVER assign only Everyday when the shirt has a formal collar and structured fabric

Blouse:
  Printed / flowy: Everyday, Party, Date Night, Travel, Festival
  Structured / silk: Work, Date Night, Party, Formal Event
  NEVER: Sport, Outdoor, Beach & Pool

Sweater:
  Fine knit / neutral: Everyday, Work, Travel, Date Night
  Chunky / cable: Everyday, Outdoor, Travel, Party
  Turtleneck: Work, Date Night, Everyday, Party, Travel
  NEVER: Sport, Beach & Pool, Festival (unless clearly oversized grunge aesthetic)

Hoodie:
  Plain / solid: Everyday, Travel, Outdoor, Sport
  Graphic / branded: Everyday, Festival, Travel, Outdoor
  NEVER: Formal Event, Work, Date Night

Tank Top:
  Athletic / racerback: Sport, Everyday, Outdoor, Beach & Pool
  Fashion / printed: Everyday, Party, Festival, Beach & Pool, Date Night
  Lace / silk-finish: Date Night, Party
  NEVER: Formal Event, Work

Crop Top:
  Basic cotton: Everyday, Festival, Travel, Beach & Pool
  Fashion / cutout: Party, Festival, Date Night, Beach & Pool
  NEVER: Formal Event, Work, Outdoor

Jeans:
  Slim / straight / dark wash: Everyday, Travel, Date Night, Work (casual office)
  Distressed / ripped: Everyday, Festival, Outdoor, Party
  Wide-leg / raw hem: Everyday, Party, Date Night, Festival, Travel
  Light wash / linen blend: Everyday, Travel, Beach & Pool (cover-up), Outdoor
  NEVER assign only Everyday — jeans are intrinsically multi-occasion

Trousers:
  Chinos / cotton: Everyday, Work, Travel, Date Night, Party
  Linen / wide-leg: Everyday, Travel, Beach & Pool (resort), Date Night, Party
  Tailored wool: Work, Formal Event, Date Night, Party
  Cargo: Everyday, Outdoor, Travel, Festival
  NEVER: Sport, Beach & Pool (unless linen resort style)

Shorts:
  Casual / cotton: Everyday, Outdoor, Travel, Beach & Pool
  Denim: Everyday, Festival, Outdoor, Travel
  Tailored / Bermuda: Everyday, Work (casual summer), Travel, Party
  Athletic / performance: Sport, Outdoor, Everyday
  NEVER: Formal Event, Work (formal), Date Night

Skirt:
  Mini: Everyday, Party, Date Night, Festival
  Midi (floral / linen): Everyday, Travel, Date Night, Party, Beach & Pool (resort)
  Maxi (flowy / boho): Everyday, Travel, Festival, Date Night, Beach & Pool, Outdoor
  Tailored pencil: Work, Formal Event, Date Night
  Pleated: Work, Date Night, Party, Everyday
  NEVER: Sport, Outdoor (except maxi), Beach & Pool (except midi/maxi resort)

Leggings:
  Athletic / performance: Sport, Outdoor, Everyday
  Fashion / printed: Everyday, Festival, Date Night, Party
  NEVER: Formal Event, Work, Beach & Pool

Joggers:
  Plain / neutral: Everyday, Travel, Outdoor, Sport
  Branded / fashion: Everyday, Travel, Party (casual), Festival
  NEVER: Formal Event, Work, Date Night, Beach & Pool

Jacket:
  Denim: Everyday, Outdoor, Travel, Festival, Party
  Leather (moto): Everyday, Party, Date Night, Festival, Outdoor
  Bomber: Everyday, Travel, Party, Festival
  Windbreaker: Outdoor, Travel, Everyday, Sport
  Blazer-cut: Work, Date Night, Party, Everyday (if unstructured)
  NEVER collapse to Everyday only — jackets are inherently multi-occasion

Coat:
  Trench: Work, Travel, Date Night, Everyday, Outdoor
  Wool overcoat: Work, Formal Event, Date Night, Travel, Everyday
  Puffer: Everyday, Outdoor, Travel, Sport
  Faux-fur: Party, Date Night, Everyday, Festival
  NEVER: Sport (unless puffer), Beach & Pool, Festival (unless faux-fur)

Blazer:
  Structured / formal: Work, Formal Event, Date Night, Party
  Unstructured / linen: Everyday, Work, Date Night, Party, Travel
  Oversized / fashion: Everyday, Party, Date Night, Festival
  NEVER: Sport, Beach & Pool, Outdoor

Vest:
  Puffer vest: Everyday, Outdoor, Travel, Sport
  Suit vest: Work, Formal Event, Date Night
  Fashion vest: Party, Festival, Everyday
  NEVER: Beach & Pool

Dress:
  Sundress / cotton / floral: Everyday, Travel, Beach & Pool, Outdoor, Party, Date Night
  Cocktail / midi satin: Party, Date Night, Formal Event
  Maxi / boho: Everyday, Festival, Travel, Beach & Pool, Date Night, Outdoor
  Little black dress: Party, Date Night, Formal Event, Work (dinner)
  Mini (casual): Everyday, Party, Date Night, Festival
  NEVER assign only one occasion to a dress — they are the most multi-occasion category

Jumpsuit:
  Casual / linen: Everyday, Travel, Party, Beach & Pool
  Tailored / structured: Work, Party, Date Night, Formal Event
  Denim / utility: Everyday, Outdoor, Festival, Travel
  NEVER: Sport, Beach & Pool (unless linen / casual)

Romper:
  Always: Everyday, Party, Beach & Pool, Travel, Festival
  NEVER: Formal Event, Work, Date Night (unless clearly fashion-forward)

Sneakers:
  Clean / minimalist: Everyday, Travel, Work (casual), Date Night (casual)
  Athletic / chunky: Everyday, Sport, Outdoor, Travel, Festival
  High-fashion / limited: Party, Festival, Date Night, Everyday
  NEVER: Formal Event, Beach & Pool

Boots:
  Ankle / leather: Everyday, Work, Date Night, Party, Travel, Outdoor
  Combat / lug-sole: Everyday, Outdoor, Festival, Party, Travel
  Over-the-knee: Date Night, Party, Everyday, Festival
  Chelsea: Work, Everyday, Date Night, Travel
  NEVER: Sport, Beach & Pool

Loafers & Oxfords:
  Classic leather: Work, Formal Event, Date Night, Everyday, Travel
  Suede / penny: Everyday, Work, Date Night, Party
  NEVER: Sport, Beach & Pool, Festival, Outdoor

Sandals:
  Flat / casual: Everyday, Travel, Beach & Pool, Outdoor, Festival
  Heeled / strappy: Date Night, Party, Everyday, Travel, Beach & Pool
  Sport sandal: Outdoor, Travel, Everyday, Beach & Pool
  NEVER: Formal Event, Work, Sport

Heels:
  Pumps / stilettos: Formal Event, Date Night, Party, Work
  Block heels: Date Night, Party, Work, Everyday
  Wedge: Everyday, Party, Travel, Date Night, Beach & Pool
  NEVER: Sport, Outdoor, Beach & Pool (stilettos)

Sports Bra:
  Always: Sport, Everyday (athleisure), Outdoor, Beach & Pool
  NEVER: Formal Event, Work, Date Night, Party

Athletic Shorts:
  Always: Sport, Outdoor, Everyday
  NEVER: Formal Event, Work, Date Night, Party, Beach & Pool (unless swimwear fabric)

Track Jacket:
  Always: Sport, Everyday, Outdoor, Travel
  NEVER: Formal Event, Work, Date Night, Beach & Pool

── OCCASION ASSIGNMENT RULES ─────────────────────────────────────────────

O1. MINIMUM 2 OCCASION IDs: Returning only Everyday for any garment that
    has structure, function, or specific aesthetics is WRONG.

O2. BEACH & POOL IS NOT A DEFAULT FOR "LIGHT" GARMENTS:
    A linen shirt is Travel + Everyday + Date Night + Outdoor — not Beach & Pool.
    Beach & Pool is only for: swimwear, cover-ups, beach dresses, sandals,
    resort rompers, and garments made explicitly for water/sun environments.
    A lightweight blouse or linen trouser is NOT Beach & Pool.

O3. EVERYDAY IS NOT A SAFE DEFAULT:
    Everyday applies only to genuinely casual, unremarkable items worn daily.
    A blazer, cocktail dress, or structured trouser is NOT Everyday unless the
    specific variant (unstructured, linen, relaxed) genuinely supports it.

O4. OCCASION UNION FROM STYLES: The final occasion list must be coherent with
    the styles assigned in Part 1. Occasions outside the union of style-valid
    occasions require explicit physical justification.

O5. FESTIVAL ≠ PARTY:
    Festival → outdoor music events, street fairs (Coachella, Burning Man energy).
    Party → indoor social gatherings, birthdays, clubs.
    Many garments qualify for both; never substitute one for the other.

O6. DATE NIGHT ≠ FORMAL EVENT:
    Date Night → intimate dinners, rooftop bars, casual-chic settings.
    Formal Event → black-tie, galas, weddings requiring strict dress codes.
    A cocktail dress qualifies for both. A ballgown does not qualify for Date Night.


═══════════════════════════════════════════════════════════════════════
STEP-BY-STEP CLASSIFICATION (reason before outputting)
═══════════════════════════════════════════════════════════════════════

Step 1: What is the exact garment type and subcategory?
         (e.g., "linen midi skirt", "heavyweight flannel shirt", "moto leather jacket")
Step 2: What is the dominant color? Convert to HSL.
Step 3: STYLE ELIMINATION — rule out all styles whose signature contradicts the garment.
         Eliminate at least 3. List the survivors.
Step 4: From the surviving styles, assign ALL that genuinely fit.
Step 5: WEATHER — inspect fabric and weight. Apply the category weather rules above.
         Assign ALL valid weather IDs (minimum 2 unless extreme garment).
Step 6: OCCASION — apply the category occasion rules + style occasion union.
         Assign ALL valid occasion IDs (minimum 2).
         Explicitly verify: "Is Beach & Pool warranted?" — only if swimwear/resort.
         Explicitly verify: "Is Everyday the only occasion?" — if yes, re-examine.
Step 7: Output the JSON.


═══════════════════════════════════════════════════════════════════════
CATALOGS
═══════════════════════════════════════════════════════════════════════

CATALOG — CATEGORIES:
{fmt(catalog.categories)}

CATALOG — STYLES:
{fmt(catalog.styles)}

CATALOG — COLORS (choose the closest match to the detected color):
{fmt(catalog.colors)}

CATALOG — WEATHER (ALL suitable conditions — minimum 2 unless extreme):
{fmt(catalog.weathers)}

CATALOG — OCCASIONS (ALL suitable occasions — minimum 2):
{fmt(catalog.occasions)}


═══════════════════════════════════════════════════════════════════════
ANTI-PATTERN EXAMPLES — DO NOT DO THIS
═══════════════════════════════════════════════════════════════════════

❌ WRONG — collapsed classification:
{{"category_id": "...", "style_id": "<any_style>", "weather_ids": ["<mild_id>"], "occasion_ids": ["<everyday_id>"]}}
Why wrong: single weather, single occasion, ignores multi-occasion potential.

❌ WRONG — Beach & Pool by default on a lightweight garment:
{{"occasion_ids": ["<beach_pool_id>"]}}
Why wrong: a linen shirt, sundress, or cotton short is Travel + Everyday + Outdoor —
Beach & Pool only applies if it's swimwear or an explicit water/sun cover-up.

❌ WRONG — Everyday as the only occasion on a structured garment:
A blazer classified as only Everyday is wrong. It must include at minimum Work + Date Night.

✅ CORRECT — diverse classification for a linen midi skirt:
Styles assigned: Prairie Romantic, Boho-Chic, Linen Breeze, Pure Minimalist (if solid color)
Weather: Hot, Warm, Mild
Occasions: Everyday, Travel, Date Night, Party, Outdoor, Festival
(Beach & Pool only if it's a resort / wrap-style skirt — otherwise excluded)


═══════════════════════════════════════════════════════════════════════
REQUIRED JSON FORMAT
═══════════════════════════════════════════════════════════════════════

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
