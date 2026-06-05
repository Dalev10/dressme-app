package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.OutfitOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos del flujo de Outfit.
 * Recibe peticiones del Gateway y delega al OutfitOrchestratorService.
 *
 * Endpoints:
 *   POST  /internal/outfit/generate              → genera outfits para el usuario
 *   GET   /internal/outfit/user/{userId}         → lista outfits del usuario
 *   GET   /internal/outfit/{outfitId}            → detalle de un outfit
 *   PATCH /internal/outfit/{outfitId}/rating     → califica un outfit (1–5)
 */
@RestController
@RequestMapping("/internal/outfit")
@RequiredArgsConstructor
@Slf4j
public class InternalOutfitController {

    private final OutfitOrchestratorService outfitService;

    @PostMapping("/generate")
    public ResponseEntity<OutfitGenerationResponse> generateOutfits(
            @RequestParam UUID userId,
            @Valid @RequestBody OutfitGenerationRequest request) {

        log.info("Back-Outfit: POST /generate — usuario {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outfitService.generateOutfits(userId, request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OutfitResponse>> getOutfits(
            @PathVariable UUID userId) {

        log.info("Back-Outfit: GET /user/{}", userId);
        return ResponseEntity.ok(outfitService.getOutfits(userId));
    }

    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitDetailResponse> getOutfitDetail(
            @PathVariable UUID outfitId) {

        log.info("Back-Outfit: GET /{}", outfitId);
        return ResponseEntity.ok(outfitService.getOutfitDetail(outfitId));
    }

    @PatchMapping("/{outfitId}/rating")
    public ResponseEntity<OutfitResponse> rateOutfit(
            @PathVariable UUID outfitId,
            @RequestParam UUID userId,
            @Valid @RequestBody OutfitRatingRequest request) {

        log.info("Back-Outfit: PATCH /{}/rating — usuario {} rating {}", outfitId, userId, request.rating());
        return ResponseEntity.ok(outfitService.rateOutfit(outfitId, userId, request));
    }
}
