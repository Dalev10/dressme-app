package com.dressme.dressme_database.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de referencia completo para los dropdowns de corrección manual
 * y los filtros del frontend.
 *
 * Devuelto en una sola llamada para evitar 4 roundtrips separados.
 * Incluye colores (faltaban en la versión anterior).
 */
public record CatalogDTO(
    List<CategoryEntry> categories,
    List<StyleEntry>    styles,
    List<ColorEntry>    colors,
    List<OccasionEntry> occasions,
    List<WeatherEntry>  weathers
) {
    public record CategoryEntry(UUID id, String name, UUID parentId) {}
    public record StyleEntry(UUID id, String name) {}
    public record ColorEntry(UUID id, String name, Integer hue, Integer saturation, Integer lightness) {}
    public record OccasionEntry(UUID id, String name) {}
    public record WeatherEntry(UUID id, String name) {}
}