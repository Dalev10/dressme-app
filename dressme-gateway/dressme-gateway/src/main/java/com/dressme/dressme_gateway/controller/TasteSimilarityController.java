package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_gateway.schema.dto.TasteSimilarityResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/taste-similarity")
@Slf4j
public class TasteSimilarityController {

    private final RestClient         backClient;
    private final JwtTokenProvider   jwtTokenProvider;

    public TasteSimilarityController(
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.backClient       = restClientBuilder.baseUrl(backendUrl).build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * POST /api/v1/taste-similarity/batch
     *
     * El controller anterior pasaba el Bearer token del usuario
     * directamente a dressme-back, que lo rechazaba porque su JwtAuthenticationFilter
     * espera un token de servicio interno (rol INTERNAL_SERVICE), no un token de usuario.
     *
     * Fix aplicado — mismo patrón que OnboardingController:
     *   1. Validar que llega un Bearer token del usuario.
     *   2. Extraer el userId del JWT del usuario.
     *   3. Generar un token interno M2M para autorizar la llamada a dressme-back.
     *   4. Pasar el userId en el body (TasteSimilarityBatchRequest ya lo contiene)
     *      y el token interno en el header Authorization.
     *
     * Nota: el userId ya viene en el body del request desde el frontend.
     * Lo extraemos del JWT como verificación de seguridad para garantizar
     * que el usuario solo puede calcular similitud con su propio taste vector.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<TasteSimilarityResponse>> computeBatch(
            @Valid @RequestBody TasteSimilarityBatchRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Gateway-TasteSimilarity: POST /batch for user {}", request.userId());
        requireBearerToken(authHeader);

        // Extraer userId del JWT y verificar que coincide con el del body
        String userToken = authHeader.substring(7);
        UUID   tokenUserId = UUID.fromString(jwtTokenProvider.getUserIdFromToken(userToken));

        if (!tokenUserId.equals(request.userId())) {
            log.warn(
                "Gateway-TasteSimilarity: userId del token ({}) no coincide con el del body ({})",
                tokenUserId, request.userId()
            );
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El userId del token no coincide con el userId del request."
            );
        }

        // Token interno M2M para autorizar la llamada a dressme-back
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        List<TasteSimilarityResponse> responses = backClient.post()
                .uri("/internal/taste-similarity/batch")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(responses);
    }

    private void requireBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway-TasteSimilarity: Petición rechazada — falta Authorization Bearer");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Se requiere un token Bearer en el header Authorization."
            );
        }
    }
}