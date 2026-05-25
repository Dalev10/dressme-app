package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_gateway.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_gateway.schema.dto.StyleCardDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Endpoints públicos de onboarding.
 * Recibe peticiones del frontend y las proxea a dressme-back.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@Slf4j
public class OnboardingController {

    private final RestClient backClient;

    public OnboardingController(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
    }

    /**
     * GET /api/v1/onboarding/style-cards
     *
     * Endpoint público — no requiere autenticación.
     * El frontend lo llama al entrar a la pantalla de onboarding.
     */
    @GetMapping("/style-cards")
    public ResponseEntity<List<StyleCardDTO>> getStyleCards() {
        log.info("Gateway-Onboarding: GET /style-cards");

        List<StyleCardDTO> cards = backClient.get()
                .uri("/internal/onboarding/style-cards")
                .retrieve()
                .body(new ParameterizedTypeReference<List<StyleCardDTO>>() {});

        return ResponseEntity.ok(cards);
    }

    /**
     * POST /api/v1/onboarding/calibrate
     *
     * Recibe las selecciones del usuario (likes/dislikes/skips)
     * y dispara el flujo completo de calibración del taste vector.
     */
    @PostMapping("/calibrate")
    public ResponseEntity<OnboardingCalibrationResponse> calibrate(
            @Valid @RequestBody OnboardingSelectionRequest request) {
        log.info("Gateway-Onboarding: POST /calibrate para usuario {}", request.userId());

        OnboardingCalibrationResponse response = backClient.post()
                .uri("/internal/onboarding/calibrate")
                .body(request)
                .retrieve()
                .body(OnboardingCalibrationResponse.class);

        return ResponseEntity.ok(response);
    }
}