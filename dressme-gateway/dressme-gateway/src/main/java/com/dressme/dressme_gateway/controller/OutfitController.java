package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.*;
import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos de Outfit.
 * Extrae el userId del JWT del usuario y lo pasa a dressme-back.
 *
 * Rutas:
 *   POST  /api/v1/outfits/generate          → genera outfits (ocasión + clima + dressCode opcional)
 *   GET   /api/v1/outfits                   → lista outfits del usuario autenticado
 *   GET   /api/v1/outfits/{id}              → detalle de un outfit
 *   PATCH /api/v1/outfits/{id}/rating       → califica un outfit (1–5)
 */
@RestController
@RequestMapping("/api/v1/outfits")
@Slf4j
public class OutfitController {

    private final RestClient backClient;
    private final JwtTokenProvider jwtTokenProvider;

    public OutfitController(
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ── Generate ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/outfits/generate
     *
     * Genera outfits para el usuario autenticado.
     * El userId se extrae del JWT — no lo envía el cliente.
     */
    @PostMapping("/generate")
    public ResponseEntity<OutfitGenerationResponse> generateOutfits(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody OutfitGenerationRequest request) {

        UUID userId = extractUserId(authHeader);
        log.info("Gateway-Outfit: POST /generate — usuario {}", userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        OutfitGenerationResponse response = backClient.post()
                .uri("/internal/outfit/generate?userId={userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(request)
                .retrieve()
                .body(OutfitGenerationResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── List ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/outfits
     *
     * Devuelve la lista de outfits del usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<List<OutfitResponse>> getOutfits(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        UUID userId = extractUserId(authHeader);
        log.info("Gateway-Outfit: GET / — usuario {}", userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        List<OutfitResponse> outfits = backClient.get()
                .uri("/internal/outfit/user/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OutfitResponse>>() {});

        return ResponseEntity.ok(outfits);
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/outfits/{outfitId}
     *
     * Devuelve el detalle completo de un outfit con sus prendas anidadas.
     */
    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitDetailResponse> getOutfitDetail(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable UUID outfitId) {

        UUID userId = extractUserId(authHeader);
        log.info("Gateway-Outfit: GET /{} — usuario {}", outfitId, userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        OutfitDetailResponse detail = backClient.get()
                .uri("/internal/outfit/{outfitId}", outfitId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(OutfitDetailResponse.class);

        return ResponseEntity.ok(detail);
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/outfits/{outfitId}/rating
     *
     * Califica un outfit del 1 al 5.
     * El userId del JWT garantiza que solo el dueño puede calificar.
     */
    @PatchMapping("/{outfitId}/rating")
    public ResponseEntity<OutfitResponse> rateOutfit(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable UUID outfitId,
            @Valid @RequestBody OutfitRatingRequest request) {

        UUID userId = extractUserId(authHeader);
        log.info("Gateway-Outfit: PATCH /{}/rating — usuario {} rating {}", outfitId, userId, request.rating());

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        OutfitResponse updated = backClient.patch()
                .uri("/internal/outfit/{outfitId}/rating?userId={userId}", outfitId, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(request)
                .retrieve()
                .body(OutfitResponse.class);

        return ResponseEntity.ok(updated);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header inválido o ausente");
        }
        String token = authHeader.substring(7);
        return UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
    }
}
