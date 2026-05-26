package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.AuditUpdateRequest;
import com.dressme.dressme_database.schema.dto.CatalogDTO;
import com.dressme.dressme_database.schema.dto.ClothingCreateRequest;
import com.dressme.dressme_database.schema.dto.ClothingItemResponse;
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
 * Endpoints internos del módulo Wardrobe.
 * Solo accesibles desde la red interna Docker (dressme-back / dressme-ai).
 * El prefijo /internal/ los deja fuera del alcance público del Gateway.
 *
 * Flujo de dos pasos:
 *   POST /internal/wardrobe          → Paso 1: registrar prenda (sin categoría)
 *   PATCH /internal/wardrobe/audit   → Paso 2: aplicar resultado Vision AI
 */
@RestController
@RequestMapping("/internal/wardrobe")
@RequiredArgsConstructor
@Slf4j
public class InternalWardrobeController {

    private final ClothingService clothingService;

    // ── Paso 1: Registro inicial de la prenda ─────────────────────────────────

    /**
     * POST /internal/wardrobe
     *
     * Registra una prenda recién subida.
     * imageUrl ya fue almacenada por dressme-back antes de llamar aquí.
     * Responde 201 Created con el ID de la prenda para que dressme-back
     * encole el trabajo de análisis Vision.
     */
    @PostMapping
    public ResponseEntity<ClothingItemResponse> createClothing(
            @Valid @RequestBody ClothingCreateRequest request) {

        log.info("Wardrobe: POST /internal/wardrobe — usuario {}", request.userId());
        ClothingItemResponse response = clothingService.createClothing(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Paso 2: Aplicar resultado del análisis Vision AI ─────────────────────

    /**
     * PATCH /internal/wardrobe/audit
     *
     * Consumido exclusivamente por dressme-ai (vía dressme-back) cuando
     * Vision termina el análisis de la imagen.
     * Actualiza category_id + is_processed y persiste tbl_clothing_ai_audit.
     */
    @PatchMapping("/audit")
    public ResponseEntity<ClothingItemResponse> applyAiAudit(
            @Valid @RequestBody AuditUpdateRequest request) {

        log.info("Wardrobe: PATCH /internal/wardrobe/audit — prenda {}",
                request.clothingId());
        ClothingItemResponse response = clothingService.applyAiAudit(request);
        return ResponseEntity.ok(response);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /**
     * GET /internal/wardrobe/user/{userId}
     *
     * Devuelve todas las prendas del guardarropa de un usuario.
     * Ordenadas por fecha de creación descendente (más recientes primero).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClothingItemResponse>> getWardrobeByUser(
            @PathVariable UUID userId) {

        log.info("Wardrobe: GET /internal/wardrobe/user/{}", userId);
        return ResponseEntity.ok(clothingService.getWardrobeByUser(userId));
    }

    /**
     * GET /internal/wardrobe/{clothingId}
     *
     * Devuelve una prenda específica por su ID.
     */
    @GetMapping("/{clothingId}")
    public ResponseEntity<ClothingItemResponse> getClothingById(
            @PathVariable UUID clothingId) {

        log.info("Wardrobe: GET /internal/wardrobe/{}", clothingId);
        return ResponseEntity.ok(clothingService.getClothingById(clothingId));
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    /**
     * DELETE /internal/wardrobe/{clothingId}?userId={userId}
     *
     * Elimina una prenda verificando que pertenece al usuario indicado.
     * userId se pasa como query param para evitar un endpoint anidado.
     */
    @DeleteMapping("/{clothingId}")
    public ResponseEntity<Void> deleteClothing(
            @PathVariable UUID clothingId,
            @RequestParam UUID userId) {

        log.info("Wardrobe: DELETE /internal/wardrobe/{} — usuario {}", clothingId, userId);
        clothingService.deleteClothing(clothingId, userId);
        return ResponseEntity.noContent().build(); // 204
    }

    // ── Catálogo de referencia ─────────────────────────────────────────────────

    /**
     * GET /internal/wardrobe/catalog
     *
     * Devuelve categorías + ocasiones + climas activos en una sola llamada.
     * Usado por dressme-back para mostrar filtros y dropdowns al frontend.
     */
    @GetMapping("/catalog")
    public ResponseEntity<CatalogDTO> getCatalog() {
        log.info("Wardrobe: GET /internal/wardrobe/catalog");
        return ResponseEntity.ok(clothingService.getCatalog());
    }
}