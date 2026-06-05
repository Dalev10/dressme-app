package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.UserTasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTasteProfileRepository extends JpaRepository<UserTasteProfile, UUID> {
    Optional<UserTasteProfile> findByUserId(UUID userId);
}