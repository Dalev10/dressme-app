package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ColorRepository extends JpaRepository<Color, UUID> {

    List<Color> findByNeutralFalse();

    /**
     * Todos los colores ordenados por nombre.
     * Usados para poblar el CatalogDTO y los dropdowns de corrección manual.
     */
    List<Color> findAllByOrderByNameAsc();

    /**
     * Búsqueda por nombre exacto (case-insensitive).
     * Permite que dressme-ai resuelva el color predicho por Vision al catálogo.
     */
    Optional<Color> findByNameIgnoreCase(String name);
}