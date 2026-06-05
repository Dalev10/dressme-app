package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ClothingAiAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClothingAiAuditRepository extends JpaRepository<ClothingAiAudit, UUID> {

    /**
     * Busca el registro de auditoría IA asociado a una prenda específica.
     * Relación 1:1 con Clothing, garantizada por @OneToOne unique=true en la entidad.
     */
    Optional<ClothingAiAudit> findByClothingId(UUID clothingId);

    /**
     * Verifica si ya existe un registro de auditoría para una prenda.
     * Útil para decidir entre INSERT y UPDATE en el flujo de análisis.
     */
    boolean existsByClothingId(UUID clothingId);
}