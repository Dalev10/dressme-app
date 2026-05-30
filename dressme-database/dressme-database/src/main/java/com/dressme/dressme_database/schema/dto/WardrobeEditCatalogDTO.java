package com.dressme.dressme_database.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo especializado para el flujo de corrección manual
 * del guardarropa.
 *
 * A diferencia de CatalogDTO, este contrato expone únicamente
 * los atributos que el usuario puede modificar:
 *
 *  - Tipo (categorías raíz)
 *  - Categoría
 *  - Estilo
 *
 * El frontend construye la jerarquía:
 *
 * Tipo
 *   ↓
 * Categoría
 *
 * utilizando parentId.
 *
 * Este DTO está completamente aislado del catálogo global
 * para evitar romper otros consumidores del dominio.
 */
public record WardrobeEditCatalogDTO(

        List<CategoryEntry> categories,

        List<StyleEntry> styles

) {

    /**
     * Categorías completas.
     *
     * parentId = null
     *      → categoría raíz (Tipo)
     *
     * parentId != null
     *      → subcategoría
     */
    public record CategoryEntry(
            UUID id,
            String name,
            UUID parentId
    ) {}

    /**
     * Estilos disponibles para selección.
     */
    public record StyleEntry(
            UUID id,
            String name
    ) {}
}