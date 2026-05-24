package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ColorRepository extends JpaRepository<Color, UUID> {
    List<Color> findByIsNeutralFalse();
}
