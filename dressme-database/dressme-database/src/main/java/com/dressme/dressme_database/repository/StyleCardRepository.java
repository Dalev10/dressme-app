package com.dressme.dressme_database.repository;
 
import com.dressme.dressme_database.model.StyleCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface StyleCardRepository extends JpaRepository<StyleCard, UUID> {
 
    /**
     * Devuelve las tarjetas activas ordenadas para la pantalla de onboarding.
     */
    List<StyleCard> findByIsActiveTrueOrderByDisplayOrderAsc();
 
    /**
     * Usado por dressme-back para obtener los embeddings de las tarjetas
     * seleccionadas y calcular el taste_vector del usuario.
     */
    List<StyleCard> findByIdIn(List<UUID> ids);
}