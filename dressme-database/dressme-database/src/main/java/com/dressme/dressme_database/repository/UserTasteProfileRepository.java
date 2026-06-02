package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.UserTasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTasteProfileRepository extends JpaRepository<UserTasteProfile, UUID> {

    Optional<UserTasteProfile> findByUserId(UUID userId);

    /**
     * Fuerza la actualización del taste vector bypaseando el dirty-checking de Hibernate.
     * El tipo @JdbcTypeCode(SqlTypes.VECTOR) no dispara dirty-checking correctamente
     * para float[], por lo que un save() estándar no genera el UPDATE.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE tbl_user_taste_profile SET taste_vector = CAST(:vector AS vector), is_calibrated = :calibrated, source_type = :sourceType, last_updated = :now WHERE user_id = :userId", nativeQuery = true)
    void updateTasteProfile(
        @Param("userId")     UUID userId,
        @Param("vector")     String vector,
        @Param("calibrated") boolean calibrated,
        @Param("sourceType") String sourceType,
        @Param("now")        LocalDateTime now
    );
}