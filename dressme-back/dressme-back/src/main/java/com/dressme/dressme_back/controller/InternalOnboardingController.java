package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.service.OnboardingOrchestratorService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping("/calibrate")
public OnboardingCalibrationResponse calibrate(
    @RequestHeader("X-User-Id") String secureUserId, // 👈 Cambiamos Jwt por el Header
    @Valid @RequestBody OnboardingSelectionRequest request
) {
    log.info("Onboarding Controller: calibración para usuario {}", secureUserId);
    
    // Ahora secureUserId ya es el string con el UUID del usuario
    return onboardingService.calibrate(secureUserId, request);
}
    public OnboardingCalibrationResponse calibrate(
        @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody OnboardingSelectionRequest request
    ) {
        String secureUserId = jwt.getSubject();
        log.info("Onboarding Controller: calibración para usuario {}", secureUserId);

        return onboardingService.calibrate(secureUserId, request);
    }
}
