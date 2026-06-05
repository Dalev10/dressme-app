package com.dressme.dressme_database.repository;
 
import com.dressme.dressme_database.model.UserOnboardingSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface UserOnboardingSelectionRepository extends JpaRepository<UserOnboardingSelection, UUID> {
 
    /**
     * Historial completo de selecciones de un usuario (útil para re-calibración futura).
     */
    List<UserOnboardingSelection> findByUserId(UUID userId);
 
    /**
     * Limpieza antes de una re-calibración: borra las selecciones previas del usuario.
     */
    void deleteByUserId(UUID userId);
}