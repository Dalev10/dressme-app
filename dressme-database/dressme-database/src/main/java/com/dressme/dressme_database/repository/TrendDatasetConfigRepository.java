package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.TrendDatasetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendDatasetConfigRepository extends JpaRepository<TrendDatasetConfig, UUID> {

    /**
     * Devuelve el vector de tendencia más reciente (el activo).
     * Cada ejecución del script inserta una nueva fila; esta query
     * siempre devuelve la más reciente sin necesidad de borrar las anteriores.
     * Las anteriores sirven como historial de versiones del dataset.
     */
    @Query(value = "SELECT * FROM tbl_trend_dataset_config ORDER BY computed_at DESC LIMIT 1", nativeQuery = true)
    Optional<TrendDatasetConfig> findLatest();
}