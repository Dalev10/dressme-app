package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ClothingOccasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClothingOccasionRepository extends JpaRepository<ClothingOccasion, UUID> {

    void deleteByClothingId(UUID clothingId);
}
