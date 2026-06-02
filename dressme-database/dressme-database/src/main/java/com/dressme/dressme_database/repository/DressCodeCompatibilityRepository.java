package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.DressCodeCompatibility;
import com.dressme.dressme_database.model.DressCodeCompatibilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DressCodeCompatibilityRepository
        extends JpaRepository<DressCodeCompatibility, DressCodeCompatibilityId> {

    List<DressCodeCompatibility> findByDressCodeId(UUID dressCodeId);
}
