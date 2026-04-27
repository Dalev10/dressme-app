package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.UserIdentity;
import com.dressme.dressme_database.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    // La consulta clave para el registro por OAuth - busca por la entidad Provider
    Optional<UserIdentity> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}