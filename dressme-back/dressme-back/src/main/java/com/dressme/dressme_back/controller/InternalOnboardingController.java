package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/onboarding")
@RequiredArgsConstructor
@Slf4j
public class InternalOnboardingController {

    private final OnboardingOrchestratorService onboardingService;

    @GetMapping("/style-cards")
    public List<StyleCardDTO> getStyleCards() {
        log.info("Onboarding Controller: solicitando style cards");
        return onboardingService.getStyleCards();
    }

    /**
     * Único método calibrate — usa X-User-Id inyectado por el Gateway.
     * El método residuo con @AuthenticationPrincipal Jwt fue eliminado:
     * era un remanente de la arquitectura anterior con JWT en el back,
     * ya reemplazada por el header X-User-Id en el Gateway.
     */
    @PostMapping("/calibrate")
    public OnboardingCalibrationResponse calibrate(
            @RequestHeader("X-User-Id") String secureUserId,
            @Valid @RequestBody OnboardingSelectionRequest request
    ) {
        log.info("Onboarding Controller: calibración para usuario {}", secureUserId);
        return onboardingService.calibrate(secureUserId, request);
    }
}