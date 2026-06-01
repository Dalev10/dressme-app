package com.dressme.dressme_back.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo especializado para la pantalla
 * de edición manual del guardarropa.
 *
 * NO reemplaza CatalogDTO.
 *
 * Su responsabilidad exclusiva es alimentar
 * los selectores:
 *
 * Tipo -> Categoría -> Estilo
 */
public record WardrobeEditCatalogDTO(

        List<CategoryEntry> categories,
        List<StyleEntry> styles

) {

    public record CategoryEntry(
            UUID id,
            String name,
            UUID parentId
    ) {}

    public record StyleEntry(
            UUID id,
            String name
    ) {}
}