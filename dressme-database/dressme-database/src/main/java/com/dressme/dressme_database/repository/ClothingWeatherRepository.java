package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ClothingWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClothingWeatherRepository extends JpaRepository<ClothingWeather, UUID> {

    void deleteByClothingId(UUID clothingId);
}
