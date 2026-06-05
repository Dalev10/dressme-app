package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Occasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OccasionRepository extends JpaRepository<Occasion, UUID> {

    /**
     * Ocasiones activas ordenadas alfabéticamente.
     * Usadas para poblar los filtros del frontend y el CatalogDTO.
     */
    List<Occasion> findByIsActiveTrueOrderByNameAsc();

    /**
     * Búsqueda por nombre exacto (case-insensitive).
     * Permite que dressme-ai resuelva la predicción al catálogo.
     */
    Optional<Occasion> findByNameIgnoreCase(String name);
}