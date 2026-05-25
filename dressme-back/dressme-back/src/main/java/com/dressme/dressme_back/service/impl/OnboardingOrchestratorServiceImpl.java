package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnboardingOrchestratorServiceImpl implements OnboardingOrchestratorService {

    private final RestClient databaseClient;
    private final RestClient aiClient;

    /**
     * Dos RestClients distintos: uno apunta a dressme-database y otro a dressme-ai.
     * Las URLs se inyectan desde las variables de entorno vía application.yml,
     * siguiendo el mismo patrón que AppConfig ya tiene en el proyecto.
     */
    public OnboardingOrchestratorServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.database-url}") String databaseUrl,
            @Value("${app.services.ai-url}") String aiUrl
    ) {
        this.databaseClient = restClientBuilder.baseUrl(databaseUrl).build();
        this.aiClient       = restClientBuilder.baseUrl(aiUrl).build();
    }

    // ── Paso A: Obtener style cards ───────────────────────────────────────────

    @Override
    public List<StyleCardDTO> getStyleCards() {
        log.info("Onboarding: Solicitando style cards a dressme-database");

        return databaseClient.get()
                .uri("/internal/onboarding/style-cards")
                .retrieve()
                .body(new ParameterizedTypeReference<List<StyleCardDTO>>() {});
    }

    // ── Flujo completo de calibración ─────────────────────────────────────────

    @Override
    public OnboardingCalibrationResponse calibrate(OnboardingSelectionRequest request) {
        log.info("Onboarding: Iniciando calibración para usuario {}", request.userId());

        // ── Paso 1: Obtener embeddings de las tarjetas seleccionadas ──────────
        // Solo pedimos los embeddings de las tarjetas que el usuario vio
        // (likes + dislikes + skips — todas las que reaccionó).
        List<UUID> selectedIds = request.selections().stream()
                .map(OnboardingSelectionRequest.SelectionItem::styleCardId)
                .collect(Collectors.toList());

        log.info("Onboarding: Solicitando embeddings de {} tarjetas a dressme-database",
                selectedIds.size());

        List<StyleCardEmbeddingResponse> embeddings = databaseClient.post()
                .uri("/internal/onboarding/style-cards/embeddings")
                .body(selectedIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "dressme-database no encontró alguna de las style cards solicitadas");
                })
                .body(new ParameterizedTypeReference<List<StyleCardEmbeddingResponse>>() {});

        // ── Paso 2: Cruzar embeddings con reacciones ──────────────────────────
        // Construimos un mapa id → embedding para hacer el join con las reacciones
        // en O(n) en lugar de O(n²).
        // Filtramos embeddings null para evitar NullPointerException en toMap()
        Map<UUID, float[]> embeddingMap = embeddings.stream()
                .filter(e -> e.embeddingVector() != null)
                .collect(Collectors.toMap(
                        StyleCardEmbeddingResponse::id,
                        StyleCardEmbeddingResponse::embeddingVector
                ));

        List<ComputeTasteVectorRequest.EmbeddingItem> items = request.selections().stream()
                .filter(sel -> embeddingMap.containsKey(sel.styleCardId()))
                .map(sel -> new ComputeTasteVectorRequest.EmbeddingItem(
                        sel.styleCardId(),
                        embeddingMap.get(sel.styleCardId()),
                        sel.reaction()
                ))
                .collect(Collectors.toList());

        // ── Paso 3: Calcular el taste vector en dressme-ai ────────────────────
        log.info("Onboarding: Enviando {} items a dressme-ai para calcular taste vector",
                items.size());

        ComputeTasteVectorRequest aiRequest = new ComputeTasteVectorRequest(
                request.userId(), items);

        ComputeTasteVectorResponse aiResponse = aiClient.post()
                .uri("/ai/onboarding/compute-taste-vector")
                .body(aiRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "dressme-ai rechazó el payload de calibración — verifica los embeddings");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new RuntimeException(
                            "dressme-ai falló al calcular el taste vector");
                })
                .body(ComputeTasteVectorResponse.class);

        log.info("Onboarding: Vector calculado — likes={}, dislikes={}",
                aiResponse.likesCount(), aiResponse.dislikesCount());

        // ── Paso 4: Persistir el taste vector en dressme-database ─────────────
        // Convertir List<Float> a float[] para UserUpdateRequest
        float[] tasteVectorArray = null;
        if (aiResponse.tasteVector() != null && !aiResponse.tasteVector().isEmpty()) {
            tasteVectorArray = new float[aiResponse.tasteVector().size()];
            for (int i = 0; i < aiResponse.tasteVector().size(); i++) {
                Float val = aiResponse.tasteVector().get(i);
                tasteVectorArray[i] = val != null ? val : 0.0f;
            }
        }

        // Reutilizamos UserUpdateRequest que ya existe en dressme-back.
        // sourceType se actualiza de "COLD_START" a "google_onboarding".
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                null,                    // displayName sin cambio
                null,                    // profilePicture sin cambio
                true,                    // isCalibrated = true
                tasteVectorArray,        // taste vector convertido a float[]
                "google_onboarding"      // sourceType actualizado
        );

        log.info("Onboarding: Persistiendo taste vector en dressme-database para usuario {}",
                request.userId());

        databaseClient.patch()
                .uri("/internal/users/{id}", request.userId())
                .body(updateRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "Error al persistir el taste vector en dressme-database");
                })
                .toBodilessEntity();

        // ── Paso 5: Persistir las selecciones del usuario ─────────────────────
        // Guardamos el historial de reacciones para posibles re-calibraciones futuras.
        // Este paso es el último porque no es bloqueante para el flujo principal:
        // si falla, el usuario ya tiene su vector calibrado.

        // Construimos el request con el mismo tipo que espera dressme-database
        var selectionsRequest = new com.dressme.dressme_back.schema.dto
                .OnboardingSelectionRequest(request.userId(), request.selections());

        log.info("Onboarding: Persistiendo {} selecciones en dressme-database",
                request.selections().size());

        databaseClient.post()
                .uri("/internal/onboarding/selections")
                .body(selectionsRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "Error al persistir las selecciones del usuario");
                })
                .toBodilessEntity();

        log.info("Onboarding: Calibración completada exitosamente para usuario {}",
                request.userId());

        return new OnboardingCalibrationResponse(
                request.userId(),
                true,
                aiResponse.tasteVector().size(),
                aiResponse.likesCount(),
                aiResponse.dislikesCount()
        );
    }
}