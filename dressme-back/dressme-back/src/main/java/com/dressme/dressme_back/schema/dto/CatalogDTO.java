package com.dressme.dressme_back.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de referencia para poblar los filtros del frontend
 * y los dropdowns de corrección manual de prendas.
 *
 * Se devuelve en una sola llamada para evitar 3 roundtrips separados
 * (categorías + ocasiones + climas).
 */
public record CatalogDTO(
    List<CategoryEntry> categories,
    List<OccasionEntry> occasions,
    List<WeatherEntry> weathers
) {

    public record CategoryEntry(UUID id, String name, UUID parentId) {}
    public record OccasionEntry(UUID id, String name) {}
    public record WeatherEntry(UUID id, String name) {}
}