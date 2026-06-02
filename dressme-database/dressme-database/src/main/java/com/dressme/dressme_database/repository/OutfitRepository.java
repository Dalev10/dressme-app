package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {
}