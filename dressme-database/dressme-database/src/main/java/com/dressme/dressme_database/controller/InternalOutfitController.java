package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.service.OutfitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos del flujo Outfit Generation en dressme-database.
 * Solo accesibles desde la red interna Docker (dressme-back).
 *
 * Endpoints:
 *   POST   /internal/outfits                    → persistir outfit generado
 *   GET    /internal/outfits/user/{userId}      → lista resumida (grid)
 *   GET    /internal/outfits/{outfitId}         → detalle completo
 *   PATCH  /internal/outfits/{outfitId}/rating  → calificar outfit
 *   DELETE /internal/outfits/{outfitId}         → eliminar outfit
 *
 * Nota: /user/{userId} declarado antes de /{outfitId} para evitar
 * que Spring interprete "user" como UUID.
 */
@RestController
@RequestMapping("/internal/outfits")
@RequiredArgsConstructor
@Slf4j
public class InternalOutfitController {

    private final OutfitService outfitService;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<OutfitResponse> createOutfit(
            @Valid @RequestBody OutfitCreateRequest request) {
        log.info("DB-Outfit: POST / — usuario {}, {} prendas",
                request.userId(), request.clothingIds().size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outfitService.createOutfit(request));
    }

    // ── Lista resumida ────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OutfitResponse>> getOutfitsByUser(
            @PathVariable UUID userId) {
        log.info("DB-Outfit: GET /user/{}", userId);
        return ResponseEntity.ok(outfitService.getOutfitsByUser(userId));
    }

    // ── Detalle completo ──────────────────────────────────────────────────────

    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitDetailResponse> getOutfitDetail(
            @PathVariable UUID outfitId) {
        log.info("DB-Outfit: GET /{}", outfitId);
        return ResponseEntity.ok(outfitService.getOutfitDetail(outfitId));
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    @PatchMapping("/{outfitId}/rating")
    public ResponseEntity<OutfitResponse> rateOutfit(
            @PathVariable UUID outfitId,
            @RequestParam  UUID userId,
            @Valid @RequestBody OutfitRatingRequest request) {
        log.info("DB-Outfit: PATCH /{}/rating — usuario {}, rating {}",
                outfitId, userId, request.rating());
        return ResponseEntity.ok(outfitService.rateOutfit(outfitId, userId, request));
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{outfitId}")
    public ResponseEntity<Void> deleteOutfit(
            @PathVariable UUID outfitId,
            @RequestParam  UUID userId) {
        log.info("DB-Outfit: DELETE /{} — usuario {}", outfitId, userId);
        outfitService.deleteOutfit(outfitId, userId);
        return ResponseEntity.noContent().build(); // 204
    }
}