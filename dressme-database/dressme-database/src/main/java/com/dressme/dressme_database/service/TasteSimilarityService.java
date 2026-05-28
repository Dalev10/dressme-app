package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.TasteSimilarityBatchRequestDTO;
import com.dressme.dressme_database.schema.dto.TasteSimilarityResponseDTO;

import java.util.List;

public interface TasteSimilarityService {
    List<TasteSimilarityResponseDTO> computeBatch(TasteSimilarityBatchRequestDTO request);
}
