package com.dressme.dressme_back.service;

import com.dressme.dressme_back.client.DatabaseTasteSimilarityClient;
import com.dressme.dressme_back.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_back.schema.dto.TasteSimilarityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TasteSimilarityService {

    private final DatabaseTasteSimilarityClient databaseTasteSimilarityClient;

    public List<TasteSimilarityResponse> computeBatch(TasteSimilarityBatchRequest request) {
        List<TasteSimilarityResponse> responses = databaseTasteSimilarityClient.computeBatch(request);

        if (responses.size() != request.outfitIds().size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Taste similarity batch response size mismatch."
            );
        }

        return responses;
    }
}
