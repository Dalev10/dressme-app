package com.dressme.dressme_gateway.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo especializado para la edición manual
 * de prendas.
 *
 * No reemplaza CatalogDTO.
 *
 * Existe únicamente para el formulario:
 *
 * Edit Clothing
 */
public record WardrobeEditCatalogDTO(

    List<TypeEntry> types,
    List<CategoryEntry> categories,
    List<StyleEntry> styles

) {

    public record TypeEntry(
            UUID id,
            String name
    ) {}

    public record CategoryEntry(
            UUID id,
            String name,
            UUID typeId
    ) {}

    public record StyleEntry(
            UUID id,
            String name
    ) {}
}