package com.dressme.dressme_gateway.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo especializado para la edición manual de prendas.
 *
 * No reemplaza CatalogDTO. Alimenta los selectores:
 * Tipo → Categoría, Estilo, Color, Ocasión, Clima.
 */
public record WardrobeEditCatalogDTO(

        List<CategoryEntry> categories,
        List<StyleEntry>    styles,
        List<ColorEntry>    colors,
        List<OccasionEntry> occasions,
        List<WeatherEntry>  weathers

) {

    public record CategoryEntry(UUID id, String name, UUID parentId) {}

    public record StyleEntry(UUID id, String name) {}

    /** hex pre-computado desde HSL para renderizar el swatch de color en la UI. */
    public record ColorEntry(UUID id, String name, String hex) {}

    public record OccasionEntry(UUID id, String name) {}

    public record WeatherEntry(UUID id, String name) {}
}