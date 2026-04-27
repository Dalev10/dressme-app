package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Útil para validaciones cruzadas o búsquedas internas
    Optional<User> findByEmail(String email);
}