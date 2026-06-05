package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StyleRepository extends JpaRepository<Style, UUID> {

    /**
     * Estilos activos ordenados alfabéticamente.
     * Usados por el frontend para filtros y dropdowns de corrección manual.
     */
    List<Style> findByIsActiveTrueOrderByNameAsc();

    /**
     * Búsqueda por nombre exacto (case-insensitive).
     * Permite que dressme-ai resuelva la predicción ("Skater", "Boho-Chic"…)
     * a la entidad correcta del catálogo.
     */
    Optional<Style> findByNameIgnoreCase(String name);
}