package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Clothing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingRepository extends JpaRepository<Clothing, UUID> {

    /**
     * Todas las prendas de un usuario, más recientes primero.
     * Usado para listar el guardarropa completo.
     */
    List<Clothing> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Prendas pendientes de análisis IA.
     * El scheduler / worker de Vision las consume en lotes.
     */
    List<Clothing> findByIsProcessedFalse();

    /**
     * Prendas de un usuario filtradas por categoría.
     * Útil cuando el frontend quiere mostrar solo "Tops", "Bottoms", etc.
     */
    List<Clothing> findByUserIdAndCategoryIdOrderByCreatedAtDesc(UUID userId, UUID categoryId);

    /**
     * Conteo rápido de prendas de un usuario (para stats del perfil).
     */
    long countByUserId(UUID userId);

    /**
     * Verifica si una prenda pertenece a un usuario concreto.
     * Evita cargar la entidad completa solo para una verificación de ownership.
     */
    @Query("SELECT COUNT(c) > 0 FROM Clothing c WHERE c.id = :clothingId AND c.user.id = :userId")
    boolean existsByIdAndUserId(@Param("clothingId") UUID clothingId, @Param("userId") UUID userId);
}