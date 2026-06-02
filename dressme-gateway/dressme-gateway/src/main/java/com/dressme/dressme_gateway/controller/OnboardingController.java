package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_gateway.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_gateway.schema.dto.StyleCardDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@Slf4j
@RequiredArgsConstructor
public class OnboardingController {

    private final RestClient backClient;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/style-cards")
    public ResponseEntity<List<StyleCardDTO>> getStyleCards() {
        log.info("Gateway-Onboarding: GET /style-cards");

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        List<StyleCardDTO> cards = backClient.get()
            .uri("/internal/onboarding/style-cards")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(cards);
    }

    @PostMapping("/calibrate")
    public ResponseEntity<OnboardingCalibrationResponse> calibrate(
        @Valid @RequestBody OnboardingSelectionRequest request,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Gateway-Onboarding: POST /calibrate");
        requireBearerToken(authHeader);

        String userToken = authHeader.substring(7);
        String userId = jwtTokenProvider.getUserIdFromToken(userToken);
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        OnboardingCalibrationResponse response = backClient.post()
            .uri("/internal/onboarding/calibrate")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
            .header("X-User-Id", userId)
            .body(request)
            .retrieve()
            .body(OnboardingCalibrationResponse.class);

        return ResponseEntity.ok(response);
    }

    private void requireBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway-Onboarding: Petición rechazada — falta Authorization Bearer");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Se requiere un token Bearer en el header Authorization."
            );
        }
    }
}