package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_gateway.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_gateway.schema.dto.StyleCardDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Gateway de onboarding. Propaga las peticiones al backend interno,
 * reenviando el token JWT del cliente para que el backend lo valide.
 *
 * El manejo de errores HTTP (401, 403, 5xx) está centralizado en
 * BackendErrorHandler vía RestClientConfig. No es necesario .onStatus() aquí.
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

    @GetMapping("/style-cards")
    public ResponseEntity<List<StyleCardDTO>> getStyleCards(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Gateway-Onboarding: GET /style-cards");
        requireBearerToken(authHeader);

        List<StyleCardDTO> cards = backClient.get()
            .uri("/internal/onboarding/style-cards")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(cards);
    }

    @PostMapping("/calibrate")
    public ResponseEntity<OnboardingCalibrationResponse> calibrate(
        @Valid @RequestBody OnboardingSelectionRequest request,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Gateway-Onboarding: POST /calibrate. Reenviando peticion al backend");
        requireBearerToken(authHeader);

        OnboardingCalibrationResponse response = backClient.post()
            .uri("/internal/onboarding/calibrate")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .body(request)
            .retrieve()
            .body(OnboardingCalibrationResponse.class);

        return ResponseEntity.ok(response);
    }

    /**
     * Validación defensiva en el Gateway antes de propagar al backend.
     * Evita llamadas al backend que sabemos que van a fallar con 401.
     */
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
