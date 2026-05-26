package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    /**
     * Condiciones climáticas activas ordenadas alfabéticamente.
     * Usadas para poblar los filtros del frontend y el CatalogDTO.
     */
    List<Weather> findByIsActiveTrueOrderByNameAsc();

    /**
     * Búsqueda por nombre exacto (case-insensitive).
     * Permite que dressme-ai resuelva la predicción al catálogo.
     */
    Optional<Weather> findByNameIgnoreCase(String name);
}