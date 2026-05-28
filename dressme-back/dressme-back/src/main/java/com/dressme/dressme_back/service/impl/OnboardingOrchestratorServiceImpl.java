package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiOnboardingClient;
import com.dressme.dressme_back.client.DatabaseOnboardingClient;
import com.dressme.dressme_back.client.DatabaseUserClient;
import com.dressme.dressme_back.schema.dto.ComputeTasteVectorRequest;
import com.dressme.dressme_back.schema.dto.ComputeTasteVectorResponse;
import com.dressme.dressme_back.schema.dto.DatabaseOnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.schema.dto.StyleCardEmbeddingResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingOrchestratorServiceImpl implements OnboardingOrchestratorService {

    private static final int EXPECTED_EMBEDDING_DIMENSIONS = 1536;

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

        UUID userId;
        try {
            userId = UUID.fromString(secureUserId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El secureUserId no es un UUID válido: " + secureUserId, e);
        }

        UserResponseDTO profile = databaseUserClient.getUserById(userId);
        if (profile == null || profile.getId() == null) {
            throw new IllegalStateException("No fue posible resolver el usuario existente");
        }

        List<UUID> selectedIds = extractStyleCardIds(request);
        List<StyleCardEmbeddingResponse> embeddings = databaseClient.getEmbeddings(selectedIds);

        List<ComputeTasteVectorRequest.EmbeddingItem> selections = buildEmbeddingItems(request, embeddings);
        if (selections.isEmpty()) {
            throw new IllegalStateException("No hay selecciones válidas para enviar al servicio de IA");
        }

        ComputeTasteVectorResponse aiResponse = null;
        boolean calibrationSuccessful = false;

        try {
            ComputeTasteVectorRequest aiRequest = new ComputeTasteVectorRequest(userId.toString(), selections);

            logAiRequestSummary(userId, selections);

            aiResponse = aiClient.computeTasteVector(aiRequest);

            log.info(
                "Onboarding: Vector calculado correctamente — likes={}, dislikes={}, cardsUsed={}",
                aiResponse.likesCount(),
                aiResponse.dislikesCount(),
                aiResponse.cardsUsed()
            );

            UserUpdateRequest updateRequest = buildUserUpdateRequest(aiResponse);
            databaseUserClient.updateUser(userId, updateRequest);

            calibrationSuccessful = true;

        } catch (FeignException e) {
            log.error(
                "Onboarding: El servicio AI rechazó la request. status={}, userId={}, body={}",
                e.status(),
                userId,
                safeFeignBody(e),
                e
            );
            throw new RuntimeException(
                "No fue posible completar la calibración. El servicio AI respondió con " + e.status(),
                e
            );
        } catch (Exception e) {
            log.error("Onboarding: Error al calcular o persistir el taste vector para usuario {}", secureUserId, e);
            throw new RuntimeException("No fue posible completar la calibración", e);
        }

        try {
            DatabaseOnboardingSelectionRequest dbRequest = buildDatabaseSelectionRequest(userId, request);
            databaseClient.saveSelections(dbRequest);
            log.info("Onboarding: Selecciones guardadas para usuario {}", userId);
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
        Map<UUID, float[]> embeddingMap = new HashMap<>();
        List<UUID> invalidEmbeddingIds = new ArrayList<>();

        for (StyleCardEmbeddingResponse embedding : embeddings) {
            float[] vector = embedding.embeddingVector();
            if (vector == null || vector.length != EXPECTED_EMBEDDING_DIMENSIONS) {
                invalidEmbeddingIds.add(embedding.id());
                continue;
            }
            embeddingMap.put(embedding.id(), vector);
        }

        List<UUID> missingIds = request.selections().stream()
            .map(OnboardingSelectionRequest.SelectionItem::styleCardId)
            .filter(id -> !embeddingMap.containsKey(id))
            .toList();

        log.info(
            "Onboarding: selected={}, embeddingsReceived={}, embeddingsValid={}, embeddingsMissing={}, invalidEmbeddingIds={}",
            request.selections().size(),
            embeddings.size(),
            embeddingMap.size(),
            missingIds.size(),
            invalidEmbeddingIds
        );

        if (!missingIds.isEmpty()) {
            throw new IllegalStateException(
                "No se encontraron embeddings válidos para estas style cards: " + missingIds
            );
        }

        List<ComputeTasteVectorRequest.EmbeddingItem> items = request.selections().stream()
            .map(sel -> {
                String normalizedReaction = normalizeReaction(sel.reaction());

                float[] vector = embeddingMap.get(sel.styleCardId());
                if (vector == null) {
                    throw new IllegalStateException(
                        "No existe embedding válido para la style card: " + sel.styleCardId()
                    );
                }

                return new ComputeTasteVectorRequest.EmbeddingItem(
                    sel.styleCardId(),
                    vector,
                    normalizedReaction
                );
            })
            .toList();

        if (items.isEmpty()) {
            throw new IllegalStateException(
                "No se pudieron construir embeddings para las selecciones del onboarding"
            );
        }

        return items;
    }

    private String normalizeReaction(String reaction) {
        if (reaction == null) {
            throw new IllegalArgumentException("La reacción no puede ser null");
        }

        String normalized = reaction.trim().toUpperCase();

        return switch (normalized) {
            case "LIKE", "DISLIKE", "SKIP" -> normalized;
            default -> throw new IllegalArgumentException(
                "Reacción inválida: " + reaction + ". Valores válidos: LIKE, DISLIKE, SKIP"
            );
        };
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
                normalizeReaction(sel.reaction())
            ))
            .toList();

        return new DatabaseOnboardingSelectionRequest(internalUserId, selections);
    }

    private void logAiRequestSummary(UUID userId, List<ComputeTasteVectorRequest.EmbeddingItem> selections) {
        List<UUID> ids = selections.stream()
            .map(ComputeTasteVectorRequest.EmbeddingItem::styleCardId)
            .toList();

        List<String> reactions = selections.stream()
            .map(ComputeTasteVectorRequest.EmbeddingItem::reaction)
            .toList();

        log.info(
            "Onboarding: enviando request a AI — userId={}, selections={}, styleCardIds={}, reactions={}",
            userId,
            selections.size(),
            ids,
            reactions
        );
    }

    private String safeFeignBody(FeignException e) {
        try {
            if (e.content() == null || e.content().length == 0) {
                return "<empty body>";
            }
            return new String(e.content(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "<unable to decode feign body>";
        }
    }
}