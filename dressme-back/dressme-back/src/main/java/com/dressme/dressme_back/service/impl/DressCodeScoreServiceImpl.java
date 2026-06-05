package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.DatabaseDressCodeClient;
import com.dressme.dressme_back.client.DatabaseOnboardingClient;
import com.dressme.dressme_back.client.DatabaseUserClient;
import com.dressme.dressme_back.client.DatabaseWardrobeClient;
import com.dressme.dressme_back.schema.dto.DressCodeDTO;
import com.dressme.dressme_back.schema.dto.OutfitDressCodeResponse;
import com.dressme.dressme_back.schema.dto.StyleCardEmbeddingResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.service.DressCodeScoreService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DressCodeScoreServiceImpl implements DressCodeScoreService {

    private static final int EXPECTED_EMBEDDING_DIMENSIONS = 1536;

    private final DatabaseWardrobeClient wardrobeClient;
    private final DatabaseUserClient userClient;
    private final DatabaseDressCodeClient dressCodeClient;
    private final DatabaseOnboardingClient onboardingClient;

    @Override
    public double score(UUID outfitId, UUID userId) {
        OutfitDressCodeResponse outfitDressCode = wardrobeClient.getDressCode(outfitId);
        UUID outfitDressCodeId = outfitDressCode.dressCodeId();
        if (outfitDressCodeId == null) {
            log.warn("DressCodeScore: outfit {} no tiene dress code asignado", outfitId);
            return 0.1;
        }

        float[] userVector = resolveUserVector(userId);
        if (userVector == null) {
            log.warn("DressCodeScore: usuario {} no tiene vector de gusto válido", userId);
            return 0.1;
        }

        List<DressCodeDTO> dressCodes = dressCodeClient.getActiveDressCodes();
        UUID inferredDressCodeId = inferDressCode(userVector, dressCodes);
        if (inferredDressCodeId == null) {
            log.warn("DressCodeScore: no se pudo inferir dress code para usuario {}", userId);
            return 0.1;
        }

        if (outfitDressCodeId.equals(inferredDressCodeId)) {
            return 1.0;
        }

        if (isCompatible(inferredDressCodeId, outfitDressCodeId)) {
            return 0.6;
        }

        return 0.1;
    }

    private float[] resolveUserVector(UUID userId) {
        try {
            UserResponseDTO user = userClient.getUserById(userId);
            if (user != null && isValidVector(user.getTasteVector())) {
                return user.getTasteVector();
            }
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No fue posible consultar el perfil del usuario",
                ex
            );
        }

        List<UUID> selectedStyleCardIds = onboardingClient.getSelectedStyleCardIds(userId);
        if (selectedStyleCardIds == null || selectedStyleCardIds.isEmpty()) {
            return null;
        }

        List<StyleCardEmbeddingResponse> embeddings = onboardingClient.getEmbeddings(selectedStyleCardIds);
        return averageEmbeddings(embeddings);
    }

    private UUID inferDressCode(float[] userVector, List<DressCodeDTO> dressCodes) {
        UUID bestDressCodeId = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (DressCodeDTO dressCode : dressCodes) {
            if (!isValidVector(dressCode.embeddingVector())) {
                continue;
            }

            double similarity = cosineSimilarity(userVector, dressCode.embeddingVector());
            if (similarity > bestScore) {
                bestScore = similarity;
                bestDressCodeId = dressCode.id();
            }
        }

        return bestDressCodeId;
    }

    private boolean isCompatible(UUID inferredDressCodeId, UUID outfitDressCodeId) {
        List<UUID> compatibleIds = dressCodeClient.getCompatibleDressCodeIds(inferredDressCodeId);
        return compatibleIds.contains(outfitDressCodeId);
    }

    private float[] averageEmbeddings(List<StyleCardEmbeddingResponse> embeddings) {
        List<float[]> validEmbeddings = new ArrayList<>();
        for (StyleCardEmbeddingResponse embedding : embeddings) {
            if (isValidVector(embedding.embeddingVector())) {
                validEmbeddings.add(embedding.embeddingVector());
            }
        }

        if (validEmbeddings.isEmpty()) {
            return null;
        }

        float[] average = new float[EXPECTED_EMBEDDING_DIMENSIONS];
        for (float[] vector : validEmbeddings) {
            for (int i = 0; i < EXPECTED_EMBEDDING_DIMENSIONS; i++) {
                average[i] += vector[i];
            }
        }

        for (int i = 0; i < EXPECTED_EMBEDDING_DIMENSIONS; i++) {
            average[i] /= validEmbeddings.size();
        }

        return average;
    }

    private boolean isValidVector(float[] vector) {
        return vector != null && vector.length == EXPECTED_EMBEDDING_DIMENSIONS;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < EXPECTED_EMBEDDING_DIMENSIONS; i++) {
            double leftValue = left[i];
            double rightValue = right[i];
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
