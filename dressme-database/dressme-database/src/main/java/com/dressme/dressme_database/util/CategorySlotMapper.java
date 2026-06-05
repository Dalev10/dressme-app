package com.dressme.dressme_database.util;

/**
 * Traduce la categoría de una prenda al "slot" lógico que usa el flujo de
 * generación de outfits (dressme-back y dressme-ai).
 *
 * Slots canónicos: TOP, BOTTOM, OUTERWEAR, FOOTWEAR, ONEPIECE.
 *
 * Contexto: la jerarquía de categorías tiene dos niveles (padre → hoja).
 * Para la mayoría de los padres el slot se deduce directamente del nombre del
 * padre (Tops → TOP, Bottoms → BOTTOM, ...). La excepción es "Activewear",
 * cuyos hijos caen en slots distintos (Sports Bra → TOP, Athletic Shorts →
 * BOTTOM, Track Jacket → OUTERWEAR); para ese caso se resuelve por la hoja.
 *
 * Devuelve {@code null} cuando la prenda no participa en un outfit: accesorios,
 * prendas sin categorizar, o cualquier categoría desconocida. El llamador debe
 * omitir esas prendas del guardarropa para generación.
 */
public final class CategorySlotMapper {

    private CategorySlotMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String TOP       = "TOP";
    public static final String BOTTOM    = "BOTTOM";
    public static final String OUTERWEAR = "OUTERWEAR";
    public static final String FOOTWEAR  = "FOOTWEAR";
    public static final String ONEPIECE  = "ONEPIECE";

    /**
     * Resuelve el slot canónico de una prenda.
     *
     * @param categoryName nombre de la categoría hoja de la prenda (ej. "T-Shirt", "Sports Bra")
     * @param parentName   nombre de la categoría padre, o {@code null} si la categoría es raíz
     * @return el slot canónico, o {@code null} si la prenda no participa en un outfit
     */
    public static String toSlot(String categoryName, String parentName) {
        // La categoría "raíz" efectiva: el padre si existe; si no, la propia categoría.
        String effectiveParent = (parentName != null && !parentName.isBlank())
                ? parentName
                : categoryName;

        if (effectiveParent == null) {
            return null;
        }

        switch (effectiveParent) {
            case "Tops":                return TOP;
            case "Bottoms":             return BOTTOM;
            case "Outerwear":           return OUTERWEAR;
            case "Footwear":            return FOOTWEAR;
            case "Dresses & Jumpsuits": return ONEPIECE;
            case "Activewear":          return activewearSlot(categoryName);
            // "Accessories", "Uncategorized" y cualquier otra → no participa en un outfit.
            default:                    return null;
        }
    }

    /**
     * Activewear es heterogéneo: cada prenda cae en un slot distinto según su tipo.
     * Se resuelve por el nombre de la categoría hoja.
     */
    private static String activewearSlot(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        switch (categoryName) {
            case "Sports Bra":      return TOP;
            case "Athletic Shorts": return BOTTOM;
            case "Track Jacket":    return OUTERWEAR;
            default:                return null;
        }
    }
}
