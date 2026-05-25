package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ColorCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ColorCompatibilityRepository extends JpaRepository<ColorCompatibility, UUID> {
    @Query("SELECT c.compatibilityScore FROM ColorCompatibility c WHERE " +
        "(c.colorA.id = :id1 AND c.colorB.id = :id2) OR " +
        "(c.colorA.id = :id2 AND c.colorB.id = :id1)")
    Optional<Double> findScoreByColorIds(@Param("id1") UUID id1, @Param("id2") UUID id2);
}
