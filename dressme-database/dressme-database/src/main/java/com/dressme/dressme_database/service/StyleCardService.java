package com.dressme.dressme_database.service;
 
import com.dressme.dressme_database.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_database.schema.dto.StyleCardDTO;
import com.dressme.dressme_database.schema.dto.StyleCardEmbeddingResponse;
 
import java.util.List;
import java.util.UUID;
 
public interface StyleCardService {
 
    /** Devuelve las tarjetas activas ordenadas para la pantalla de onboarding. */
    List<StyleCardDTO> getActiveStyleCards();
 
    /**
     * Devuelve los embeddings de un subconjunto de tarjetas.
     * Usado internamente por dressme-back para calcular el taste_vector.
     */
    List<StyleCardEmbeddingResponse> getEmbeddingsByIds(List<UUID> ids);
 
    /**
     * Persiste las selecciones del usuario.
     * Si el usuario ya tenía selecciones previas, las reemplaza (re-calibración).
     */
    void saveSelections(OnboardingSelectionRequest request);
}