package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.ClothingOutfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingOutfitRepository extends JpaRepository<ClothingOutfit, UUID> {

    /**
     * Todas las asociaciones prenda-outfit de un outfit dado.
     * Usado para construir OutfitDetailResponse con las prendas completas.
     */
    List<ClothingOutfit> findByOutfitId(UUID outfitId);

    /**
     * Todos los outfits en los que aparece una prenda concreta.
     * Permite invalidar outfits cuando el usuario elimina una prenda.
     */
    List<ClothingOutfit> findByClothingId(UUID clothingId);

    /**
     * Elimina todas las asociaciones de un outfit antes de borrarlo.
     * Evita FK violations al eliminar el outfit padre.
     */
    void deleteByOutfitId(UUID outfitId);
}