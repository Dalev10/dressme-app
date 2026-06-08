package com.dressme.dressme_database.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo especializado para el flujo de corrección manual
 * del guardarropa.
 *
 * Expone los atributos que el usuario puede modificar:
 * Tipo/Categoría, Estilo, Color, Ocasión y Clima.
 *
 * Este DTO está completamente aislado del catálogo global
 * para evitar romper otros consumidores del dominio.
 */
public record WardrobeEditCatalogDTO(

        List<CategoryEntry> categories,
        List<StyleEntry>    styles,
        List<ColorEntry>    colors,
        List<OccasionEntry> occasions,
        List<WeatherEntry>  weathers

) {

    /** parentId = null → categoría raíz (Tipo); parentId != null → subcategoría */
    public record CategoryEntry(UUID id, String name, UUID parentId) {}

    public record StyleEntry(UUID id, String name) {}

    /** hex pre-computado desde HSL para renderizar el swatch de color en la UI. */
    public record ColorEntry(UUID id, String name, String hex) {}

    public record OccasionEntry(UUID id, String name) {}

    public record WeatherEntry(UUID id, String name) {}
}