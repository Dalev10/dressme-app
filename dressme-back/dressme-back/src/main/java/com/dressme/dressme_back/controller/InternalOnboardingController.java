package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints internos de onboarding.
 * Recibe peticiones del Gateway y delega al OnboardingOrchestratorService.
 * Mismo patrón que InternalOrchestratorController.
 */
@RestController
@RequestMapping("/internal/onboarding")
@RequiredArgsConstructor
@Slf4j
public class InternalOnboardingController {

    private final OnboardingOrchestratorService onboardingService;

    /**
     * GET /internal/onboarding/style-cards
     *
     * Devuelve las style cards activas para la pantalla de onboarding.
     * El Gateway lo expone como GET /api/v1/onboarding/style-cards.
     */
    @GetMapping("/style-cards")
    public ResponseEntity<List<StyleCardDTO>> getStyleCards() {
        log.info("Back-Onboarding: Solicitud de style cards recibida");
        return ResponseEntity.ok(onboardingService.getStyleCards());
    }

    /**
     * POST /internal/onboarding/calibrate
     *
     * Orquesta el flujo completo de calibración del taste vector.
     * El Gateway lo expone como POST /api/v1/onboarding/calibrate.
     */
    @PostMapping("/calibrate")
    public ResponseEntity<OnboardingCalibrationResponse> calibrate(
            @Valid @RequestBody OnboardingSelectionRequest request) {
        log.info("Back-Onboarding: Solicitud de calibración recibida para usuario {}",
                request.userId());
        return ResponseEntity.ok(onboardingService.calibrate(request));
    }
}