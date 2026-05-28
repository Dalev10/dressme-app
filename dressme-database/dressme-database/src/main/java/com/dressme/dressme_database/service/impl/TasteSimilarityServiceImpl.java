package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.repository.OutfitAiAuditRepository;
import com.dressme.dressme_database.repository.UserTasteProfileRepository;
import com.dressme.dressme_database.schema.dto.TasteSimilarityBatchRequestDTO;
import com.dressme.dressme_database.schema.dto.TasteSimilarityResponseDTO;
import com.dressme.dressme_database.service.TasteSimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteSimilarityServiceImpl implements TasteSimilarityService {

    private final UserTasteProfileRepository userTasteProfileRepository;
    private final OutfitAiAuditRepository outfitAiAuditRepository;

    @Override
    public List<TasteSimilarityResponseDTO> computeBatch(TasteSimilarityBatchRequestDTO request) {
        List<UUID> outfitIds = request.outfitIds();

        if (userTasteProfileRepository.findByUserId(request.userId()).isEmpty()) {
            return buildMissingResponses(outfitIds);
        }

        List<OutfitAiAuditRepository.TasteSimilarityProjection> projections =
            outfitAiAuditRepository.findTasteSimilarities(request.userId(), outfitIds);

        Map<UUID, Double> similarityByOutfit = new HashMap<>();
        for (OutfitAiAuditRepository.TasteSimilarityProjection projection : projections) {
            similarityByOutfit.put(projection.getOutfitId(), projection.getSimilarity());
        }

        List<TasteSimilarityResponseDTO> responses = new ArrayList<>(outfitIds.size());
        for (UUID outfitId : outfitIds) {
            Double similarity = similarityByOutfit.get(outfitId);
            if (similarity == null) {
                responses.add(new TasteSimilarityResponseDTO(outfitId, 0.0, false));
            } else {
                responses.add(new TasteSimilarityResponseDTO(outfitId, similarity, true));
            }
        }

        return responses;
    }

    private List<TasteSimilarityResponseDTO> buildMissingResponses(List<UUID> outfitIds) {
        List<TasteSimilarityResponseDTO> responses = new ArrayList<>(outfitIds.size());
        for (UUID outfitId : outfitIds) {
            responses.add(new TasteSimilarityResponseDTO(outfitId, 0.0, false));
        }
        return responses;
    }
}
