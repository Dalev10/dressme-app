package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiOnboardingClient;
import com.dressme.dressme_back.client.DatabaseOnboardingClient;
import com.dressme.dressme_back.client.DatabaseUserClient;
import com.dressme.dressme_back.schema.dto.ComputeTasteVectorRequest;
import com.dressme.dressme_back.schema.dto.ComputeTasteVectorResponse;
import com.dressme.dressme_back.schema.dto.DatabaseOnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.schema.dto.StyleCardEmbeddingResponse;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingOrchestratorServiceImpl implements OnboardingOrchestratorService {

    private final AiOnboardingClient aiClient;
    private final DatabaseOnboardingClient databaseClient;
    private final DatabaseUserClient databaseUserClient;

    @Override
    public List<StyleCardDTO> getStyleCards() {
        log.info("Onboarding: Solicitando style cards a dressme-database");
        return databaseClient.getStyleCards();
    }

    @Override
    public OnboardingCalibrationResponse calibrate(String secureUserId, OnboardingSelectionRequest request) {
        log.info("Onboarding: Iniciando calibración para usuario externo {}", secureUserId);

        InternalUserCreateRequest createRequest = new InternalUserCreateRequest(
            null,
            null,
            null,
            "google",
            secureUserId,
            null
        );
        UserProfileResponse profile = databaseUserClient.createOrFetchUser(createRequest);

        if (profile == null || profile.id() == null) {
            throw new IllegalStateException("No fue posible resolver el usuario interno");
        }

        UUID internalUserId = profile.id();
        log.info("Onboarding: Usuario interno resuelto {}", internalUserId);

        List<UUID> selectedIds = extractStyleCardIds(request);
        List<StyleCardEmbeddingResponse> embeddings = databaseClient.getEmbeddings(selectedIds);

        List<ComputeTasteVectorRequest.EmbeddingItem> selections = buildEmbeddingItems(request, embeddings);

        ComputeTasteVectorResponse aiResponse = null;
        boolean calibrationSuccessful = false;

        try {
            ComputeTasteVectorRequest aiRequest =
                new ComputeTasteVectorRequest(internalUserId.toString(), selections);

            aiResponse = aiClient.computeTasteVector(aiRequest);

            log.info("Onboarding: Vector calculado — likes={}, dislikes={}",
                aiResponse.likesCount(), aiResponse.dislikesCount());

            UserUpdateRequest updateRequest = buildUserUpdateRequest(aiResponse);
            databaseUserClient.updateUser(internalUserId, updateRequest);

            calibrationSuccessful = true;

        } catch (Exception e) {
            log.error("Onboarding: Error al calcular o persistir el taste vector para usuario externo {}",
                secureUserId, e);
        }

        try {
            DatabaseOnboardingSelectionRequest dbRequest =
                buildDatabaseSelectionRequest(internalUserId, request);

            databaseClient.saveSelections(dbRequest);
            log.info("Onboarding: Selecciones guardadas para usuario interno {}", internalUserId);

        } catch (Exception e) {
            log.error("Onboarding: Error crítico al guardar selecciones en dressme-database", e);
            throw e;
        }

        return buildCalibrationResponse(secureUserId, calibrationSuccessful, aiResponse);
    }

    private OnboardingCalibrationResponse buildCalibrationResponse(
        String secureUserId,
        boolean calibrationSuccessful,
        ComputeTasteVectorResponse aiResponse
    ) {
        if (calibrationSuccessful && aiResponse != null) {
            return new OnboardingCalibrationResponse(
                secureUserId,
                true,
                aiResponse.tasteVector() != null ? aiResponse.tasteVector().size() : 0,
                aiResponse.likesCount(),
                aiResponse.dislikesCount()
            );
        }

        return new OnboardingCalibrationResponse(secureUserId, false, 0, 0, 0);
    }

    private List<UUID> extractStyleCardIds(OnboardingSelectionRequest request) {
        return request.selections().stream()
            .map(OnboardingSelectionRequest.SelectionItem::styleCardId)
            .toList();
    }

    private List<ComputeTasteVectorRequest.EmbeddingItem> buildEmbeddingItems(
        OnboardingSelectionRequest request,
        List<StyleCardEmbeddingResponse> embeddings
    ) {
        Map<UUID, float[]> embeddingMap = embeddings.stream()
            .filter(e -> e.embeddingVector() != null)
            .collect(Collectors.toMap(
                StyleCardEmbeddingResponse::id,
                StyleCardEmbeddingResponse::embeddingVector
            ));

        return request.selections().stream()
            .filter(sel -> embeddingMap.containsKey(sel.styleCardId()))
            .map(sel -> new ComputeTasteVectorRequest.EmbeddingItem(
                sel.styleCardId(),
                embeddingMap.get(sel.styleCardId()),
                sel.reaction()
            ))
            .toList();
    }

    private UserUpdateRequest buildUserUpdateRequest(ComputeTasteVectorResponse aiResponse) {
        float[] tasteVectorArray = null;

        if (aiResponse.tasteVector() != null && !aiResponse.tasteVector().isEmpty()) {
            tasteVectorArray = new float[aiResponse.tasteVector().size()];
            for (int i = 0; i < aiResponse.tasteVector().size(); i++) {
                Float val = aiResponse.tasteVector().get(i);
                tasteVectorArray[i] = val != null ? val : 0.0f;
            }
        }

        return new UserUpdateRequest(null, null, true, tasteVectorArray, "google_onboarding");
    }

    private DatabaseOnboardingSelectionRequest buildDatabaseSelectionRequest(
        UUID internalUserId,
        OnboardingSelectionRequest request
    ) {
        List<DatabaseOnboardingSelectionRequest.SelectionItem> selections = request.selections().stream()
            .map(sel -> new DatabaseOnboardingSelectionRequest.SelectionItem(
                sel.styleCardId(),
                sel.reaction()
            ))
            .toList();

        return new DatabaseOnboardingSelectionRequest(internalUserId, selections);
    }
}
