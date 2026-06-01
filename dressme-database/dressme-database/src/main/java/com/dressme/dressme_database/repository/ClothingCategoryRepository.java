package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ClothingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClothingCategoryRepository extends JpaRepository<ClothingCategory, UUID> {

    /**
     * Categorías padre activas (las que el frontend muestra como filtros de primer nivel).
     * parent_id = NULL indica que son raíces de la jerarquía.
     */
    @Query("SELECT c FROM ClothingCategory c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.name ASC")
    List<ClothingCategory> findActiveRootCategories();

    /**
     * Subcategorías activas de un padre dado.
     * Ej: pasar el ID de "Tops" devuelve T-Shirt, Shirt, Blouse, etc.
     */
    List<ClothingCategory> findByParentIdAndIsActiveTrueOrderByNameAsc(UUID parentId);

    /**
     * Todas las categorías activas, planas.
     * Usadas por dressme-ai para mapear la predicción de Vision al catálogo.
     */
    List<ClothingCategory> findByIsActiveTrueOrderByNameAsc();

    /**
     * Búsqueda por nombre exacto (case-insensitive).
     * Permite que dressme-ai resuelva "T-Shirt" → entidad correcta.
     */
    Optional<ClothingCategory> findByNameIgnoreCase(String name);

    Optional<ClothingCategory> findByIdAndParentIdAndIsActiveTrue(
        UUID categoryId,
        UUID parentId
    );
}