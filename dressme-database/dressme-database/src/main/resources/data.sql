-- =============================================================================
-- SEED: tbl_style_cards
-- 12 tarjetas de onboarding para el flujo de caracterización de usuario.
--
-- IMPORTANTE sobre embedding_vector:
--   Se inserta como NULL. La Fase 2 (dressme-ai) ejecutará un script one-shot
--   que llama a OpenAI text-embedding-3-small con semantic_description y
--   actualiza este campo por cada fila.
--
-- IMPORTANTE sobre el conflicto en re-arranques:
--   Usamos INSERT ... ON CONFLICT DO NOTHING para que el seed sea idempotente.
--   Spring Boot carga data.sql en cada arranque cuando spring.sql.init.mode=always.
-- =============================================================================

INSERT INTO tbl_style_cards
    (id, name, semantic_description, image_url, tags, embedding_vector, display_order, is_active)
VALUES

-- 1. Minimalist
(
    '11111111-0000-0000-0000-000000000001',
    'Minimalist',
    'Clean lines, neutral tones, and deliberate simplicity. Outfits built around white, beige, grey and black basics. Structured silhouettes, quality fabrics, no excess. Less is more: one focal piece, the rest understated. Capsule wardrobe essentials: slim trousers, fitted turtlenecks, tailored blazers, leather accessories.',
    'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600',
    '["neutral", "clean", "structured", "capsule", "monochrome"]',
    NULL,
    1,
    true
),

-- 2. Streetwear
(
    '11111111-0000-0000-0000-000000000002',
    'Streetwear',
    'Urban culture meets fashion. Oversized hoodies, graphic tees, cargo pants, chunky sneakers. Bold logos, drop-shoulder fits, layered looks. Influences from skateboarding, hip-hop, and sportswear. Key pieces: Air Jordans, bomber jackets, beanies, crossbody bags, wide-leg jeans with distressed details.',
    'https://images.unsplash.com/photo-1552374196-1ab2a1c593e8?w=600',
    '["urban", "oversized", "graphic", "sneakers", "casual", "youth"]',
    NULL,
    2,
    true
),

-- 3. Classic & Formal
(
    '11111111-0000-0000-0000-000000000003',
    'Classic & Formal',
    'Timeless elegance rooted in traditional tailoring. Suits, blazers, oxford shirts, pencil skirts, trench coats. Muted palette: navy, charcoal, cream, burgundy. Polished shoes, structured bags, minimal jewellery. Dress codes: business formal, cocktail, black tie optional. Think Brooks Brothers, Ralph Lauren, Burberry.',
    'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600',
    '["formal", "tailored", "office", "elegant", "traditional", "business"]',
    NULL,
    3,
    true
),

-- 4. Bohemian
(
    '11111111-0000-0000-0000-000000000004',
    'Bohemian',
    'Free-spirited and artistic. Flowy maxi dresses, crochet tops, wide-brim hats, fringe details. Earthy palette: terracotta, mustard, sage green, rust. Natural fabrics like linen, cotton, and suede. Layered necklaces, stacked rings, woven bags. Festival vibes, nature-inspired prints, vintage finds and artisanal pieces.',
    'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=600',
    '["boho", "earthy", "flowy", "vintage", "artisanal", "festival", "natural"]',
    NULL,
    4,
    true
),

-- 5. Athleisure
(
    '11111111-0000-0000-0000-000000000005',
    'Athleisure',
    'Performance meets everyday style. Leggings, joggers, sports bras, hoodies, and track jackets worn beyond the gym. Technical fabrics: spandex, moisture-wicking, seamless knit. Clean athletic silhouettes, neutral and bold sport colors. Brands: Nike, Lululemon, Adidas. Effortless and functional, from workout to brunch.',
    'https://images.unsplash.com/photo-1556906781-9a412961a28c?w=600',
    '["sport", "gym", "comfort", "functional", "activewear", "casual", "performance"]',
    NULL,
    5,
    true
),

-- 6. Dark Academia
(
    '11111111-0000-0000-0000-000000000006',
    'Dark Academia',
    'Literary and scholarly aesthetic rooted in gothic European architecture. Tweed blazers, plaid trousers, turtleneck sweaters, pleated skirts, loafers and oxford shoes. Dark palette: deep brown, forest green, burgundy, black, cream. Layered and bookish. Think Oxford libraries, candlelit studies, worn leather satchels.',
    'https://images.unsplash.com/photo-1544816155-12df9643f363?w=600',
    '["gothic", "scholarly", "plaid", "tweed", "vintage", "intellectual", "dark", "preppy"]',
    NULL,
    6,
    true
),

-- 7. Y2K / Retro
(
    '11111111-0000-0000-0000-000000000007',
    'Y2K / Retro',
    'Early 2000s nostalgia reimagined. Low-rise jeans, baby tees, mini skirts, platform shoes, butterfly clips, metallic fabrics. Bold candy colors: hot pink, electric blue, lime green, silver. Influences from pop icons like Britney Spears and Paris Hilton. Glossy, playful, unapologetically fun and maximalist.',
    'https://images.unsplash.com/photo-1612336307429-8a898d10e223?w=600',
    '["retro", "2000s", "playful", "maximalist", "colorful", "nostalgia", "pop"]',
    NULL,
    7,
    true
),

-- 8. Cottagecore
(
    '11111111-0000-0000-0000-000000000008',
    'Cottagecore',
    'Romanticized rural life inspired by English countryside and fairy tales. Floral prints, puffed sleeves, prairie dresses, pinafores, lace collars. Soft pastel palette: blush pink, lavender, mint, ivory. Natural textures: cotton voile, linen, broderie anglaise. Basket bags, Mary Jane shoes, wildflower headpieces. Idyllic, feminine and whimsical.',
    'https://images.unsplash.com/photo-1585386959984-a4155224a1ad?w=600',
    '["floral", "feminine", "romantic", "pastel", "nature", "vintage", "whimsical", "rural"]',
    NULL,
    8,
    true
),

-- 9. Business Casual
(
    '11111111-0000-0000-0000-000000000009',
    'Business Casual',
    'Professional without being stiff. Chinos, knit polos, button-downs, blazers over turtlenecks, loafers and clean white sneakers. Neutral palette with color accents: camel, slate blue, olive, off-white. Smart-casual balance suitable for hybrid workplaces. Approachable yet put-together. Neat, well-fitted, versatile.',
    'https://images.unsplash.com/photo-1512374382149-233c42b6a83b?w=600',
    '["office", "smart-casual", "professional", "versatile", "neat", "work"]',
    NULL,
    9,
    true
),

-- 10. Coastal / Nautical
(
    '11111111-0000-0000-0000-000000000010',
    'Coastal',
    'Relaxed beach-inspired wardrobe. Linen shirts, striped breton tops, wide-leg linen trousers, espadrilles, raffia bags. Maritime palette: navy, white, sand, coral, sky blue. Lightweight and breathable fabrics. Effortless Mediterranean summer energy. Sun-kissed, unhurried, salt-in-the-air aesthetic.',
    'https://images.unsplash.com/photo-1471286174890-9c112ffca5b4?w=600',
    '["beach", "summer", "linen", "nautical", "relaxed", "coastal", "mediterranean"]',
    NULL,
    10,
    true
),

-- 11. Edgy / Avant-Garde
(
    '11111111-0000-0000-0000-000000000011',
    'Edgy & Avant-Garde',
    'Boundary-pushing fashion that treats clothing as art. Deconstructed silhouettes, asymmetric cuts, unexpected textures: latex, mesh, PVC, studded leather. Dark palette punctuated by bold accents. Mixing high fashion with underground culture. Influences: Rick Owens, Vivienne Westwood, Comme des Garçons. Provocative, conceptual, unapologetic.',
    'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600',
    '["edgy", "dark", "leather", "punk", "avant-garde", "bold", "unconventional", "fashion-forward"]',
    NULL,
    11,
    true
),

-- 12. Smart Casual
(
    '11111111-0000-0000-0000-000000000012',
    'Smart Casual',
    'The everyday uniform of modern life. Dark jeans or chinos, clean crewneck sweaters, quality sneakers or Derby shoes, minimal accessories. Balanced between relaxed and polished. Neutral base colors: white, grey, navy, black. Easy to dress up or down. Works for dinners, casual Fridays, weekend outings. Timeless and effortless.',
    'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=600',
    '["everyday", "versatile", "balanced", "casual", "modern", "effortless", "clean"]',
    NULL,
    12,
    true
)

ON CONFLICT DO NOTHING;

-- =============================================================================
-- SEED: tbl_colors
-- 30 colores base para el motor de combinaciones y recomendaciones.
-- Valores HSL (Hue: 0-360, Saturation: 0-100, Lightness: 0-100)
-- =============================================================================

INSERT INTO tbl_colors (id, name, hue, saturation, lightness, is_neutral)
VALUES
-- === NEUTROS (is_neutral = true) ===
('22222222-0000-0000-0000-000000000001', 'Black', 0, 0, 0, true),
('22222222-0000-0000-0000-000000000002', 'Dark Gray', 0, 0, 25, true),
('22222222-0000-0000-0000-000000000003', 'Gray', 0, 0, 50, true),
('22222222-0000-0000-0000-000000000004', 'Light Gray', 0, 0, 75, true),
('22222222-0000-0000-0000-000000000005', 'White', 0, 0, 100, true),
('22222222-0000-0000-0000-000000000006', 'Cream', 40, 100, 95, true),
('22222222-0000-0000-0000-000000000007', 'Beige', 35, 25, 85, true),
('22222222-0000-0000-0000-000000000008', 'Taupe', 30, 15, 50, true),

-- === CÁLIDOS (is_neutral = false) ===
('22222222-0000-0000-0000-000000000009', 'Red', 0, 100, 50, false),
('22222222-0000-0000-0000-000000000010', 'Crimson', 348, 83, 47, false),
('22222222-0000-0000-0000-000000000011', 'Maroon', 348, 83, 25, false),
('22222222-0000-0000-0000-000000000012', 'Orange', 30, 100, 50, false),
('22222222-0000-0000-0000-000000000013', 'Coral', 16, 100, 66, false),
('22222222-0000-0000-0000-000000000014', 'Yellow', 60, 100, 50, false),
('22222222-0000-0000-0000-000000000015', 'Gold', 51, 100, 50, false),
('22222222-0000-0000-0000-000000000016', 'Mustard', 45, 100, 40, false),
('22222222-0000-0000-0000-000000000017', 'Brown', 30, 100, 25, false),
('22222222-0000-0000-0000-000000000018', 'Rust', 14, 78, 40, false),

-- === FRÍOS (is_neutral = false) ===
('22222222-0000-0000-0000-000000000019', 'Green', 120, 100, 25, false),
('22222222-0000-0000-0000-000000000020', 'Lime', 120, 100, 50, false),
('22222222-0000-0000-0000-000000000021', 'Forest Green', 120, 61, 34, false),
('22222222-0000-0000-0000-000000000022', 'Olive', 60, 100, 25, false),
('22222222-0000-0000-0000-000000000023', 'Cyan', 180, 100, 50, false),
('22222222-0000-0000-0000-000000000024', 'Teal', 180, 100, 25, false),
('22222222-0000-0000-0000-000000000025', 'Blue', 240, 100, 50, false),
('22222222-0000-0000-0000-000000000026', 'Sky Blue', 197, 71, 73, false),
('22222222-0000-0000-0000-000000000027', 'Navy', 240, 100, 25, false),

-- === MAGENTAS Y PÚRPURAS (is_neutral = false) ===
('22222222-0000-0000-0000-000000000028', 'Purple', 300, 100, 25, false),
('22222222-0000-0000-0000-000000000029', 'Lavender', 240, 67, 94, false),
('22222222-0000-0000-0000-000000000030', 'Pink', 350, 100, 88, false)

ON CONFLICT DO NOTHING;

-- =============================================================================
-- SEED: tbl_providers
-- OAuth providers soportados por la aplicación. Por ahora solo GOOGLE.
-- =============================================================================

INSERT INTO tbl_providers (id, name, auth_endpoint, icon_url, is_active)
VALUES ('33333333-0000-0000-0000-000000000001', 'GOOGLE', 'https://accounts.google.com/o/oauth2/auth', 'https://www.gstatic.com/images/branding/product/1x/gboard_color_48dp.png', true)
ON CONFLICT DO NOTHING;