package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.service.ClothingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD completo del Wardrobe en dressme-database.
 * Solo accesible desde la red interna Docker.
 *
 * Endpoints:
 *   POST   /internal/wardrobe                      → Paso 1: registrar prenda
 *   PATCH  /internal/wardrobe/audit                → Paso 2: audit Vision IA
 *   GET    /internal/wardrobe/user/{userId}        → lista resumida (grid)
 *   GET    /internal/wardrobe/{clothingId}         → detalle compuesto (view/edit)
 *   PATCH  /internal/wardrobe/{clothingId}         → corrección manual del usuario
 *   DELETE /internal/wardrobe/{clothingId}         → eliminar prenda
 *   GET    /internal/wardrobe/catalog              → catálogo de filtros
 */
@RestController
@RequestMapping("/internal/wardrobe")
@RequiredArgsConstructor
@Slf4j
public class InternalWardrobeController {

    private final ClothingService clothingService;

    // ── Paso 1: Registro inicial ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ClothingItemResponse> createClothing(
            @Valid @RequestBody ClothingCreateRequest request) {
        log.info("DB-Wardrobe: POST / — usuario {}", request.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clothingService.createClothing(request));
    }

    // ── Paso 2: Audit Vision ──────────────────────────────────────────────────

    @PatchMapping("/audit")
    public ResponseEntity<ClothingItemResponse> applyAiAudit(
            @Valid @RequestBody AuditUpdateRequest request) {
        log.info("DB-Wardrobe: PATCH /audit — clothingId={}, predictedColorId={}, detectedHue={}, detectedSaturation={}, detectedLightness={}",
                request.clothingId(),
                request.predictedColorId(),
                request.detectedHue(),
                request.detectedSaturation(),
                request.detectedLightness());
        log.info("DB-Wardrobe: PATCH /audit — prenda {}", request.clothingId());
        return ResponseEntity.ok(clothingService.applyAiAudit(request));
    }

    // ── Lista resumida (grid del guardarropa) ─────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClothingItemResponse>> getWardrobe(
            @PathVariable UUID userId) {
        log.info("DB-Wardrobe: GET /user/{}", userId);
        return ResponseEntity.ok(clothingService.getWardrobeByUser(userId));
    }

    // ── Detalle compuesto (vista prenda + audit) ──────────────────────────────

    /**
     * GET /internal/wardrobe/{clothingId}
     *
     * Devuelve ClothingDetailResponse: prenda + todos los campos del audit IA.
     * Usado para la vista de detalle y para pre-poblar el formulario de edición.
     *
     * Nota: todos los campos del audit son null si isProcessed=false.
     */
    @GetMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> getClothingDetail(
            @PathVariable UUID clothingId) {
        log.info("DB-Wardrobe: GET /{}", clothingId);
        return ResponseEntity.ok(clothingService.getClothingDetail(clothingId));
    }

    // ── Corrección manual ─────────────────────────────────────────────────────

    /**
     * PATCH /internal/wardrobe/{clothingId}?userId={userId}
     *
     * El usuario corrige los campos predichos por la IA.
     * Actualiza tbl_clothes + tbl_clothing_ai_audit (was_corrected=true).
     * Devuelve el detalle compuesto actualizado para que el frontend
     * refleje los cambios sin necesidad de un GET adicional.
     */
    @PatchMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> updateClothing(
            @PathVariable UUID clothingId,
            @RequestParam  UUID userId,
            @Valid @RequestBody ClothingUpdateRequest request) {
        log.info("DB-Wardrobe: PATCH /{} — usuario {}", clothingId, userId);
        return ResponseEntity.ok(
                clothingService.updateClothing(clothingId, userId, request));
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    @DeleteMapping("/{clothingId}")
    public ResponseEntity<Void> deleteClothing(
            @PathVariable UUID clothingId,
            @RequestParam  UUID userId) {
        log.info("DB-Wardrobe: DELETE /{} — usuario {}", clothingId, userId);
        clothingService.deleteClothing(clothingId, userId);
        return ResponseEntity.noContent().build(); // 204
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    @GetMapping("/catalog")
    public ResponseEntity<CatalogDTO> getCatalog() {
        log.info("DB-Wardrobe: GET /catalog");
        return ResponseEntity.ok(clothingService.getCatalog());
    }
}