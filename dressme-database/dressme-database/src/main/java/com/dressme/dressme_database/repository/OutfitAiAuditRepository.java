package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.OutfitAiAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutfitAiAuditRepository extends JpaRepository<OutfitAiAudit, UUID> {

    interface TasteSimilarityProjection {
        UUID getOutfitId();
        Double getSimilarity();
    }

    @Query(value = """
        select a.outfit_id as outfitId,
               case
                   when a.outfit_vector is null then null
                   else greatest(0, least(1, 1 - (u.taste_vector <=> a.outfit_vector)))
               end as similarity
          from tbl_outfit_ai_audit a
          join tbl_user_taste_profile u on u.user_id = :userId
         where a.outfit_id in (:outfitIds)
        """, nativeQuery = true)
    List<TasteSimilarityProjection> findTasteSimilarities(
        @Param("userId") UUID userId,
        @Param("outfitIds") List<UUID> outfitIds
    );
}
