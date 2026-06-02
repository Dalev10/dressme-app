package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.DressCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DressCodeRepository extends JpaRepository<DressCode, UUID> {
    List<DressCode> findByIsActiveTrueOrderByNameAsc();
}