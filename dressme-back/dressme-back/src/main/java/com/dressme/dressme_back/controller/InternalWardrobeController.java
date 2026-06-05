package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.WardrobeOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * CRUD completo del Wardrobe 
 * Recibe peticiones del Gateway y delega al WardrobeOrchestratorService.
 *
 * Endpoints:
 *   POST   /internal/wardrobe/upload          → subir prenda (multipart)
 *   GET    /internal/wardrobe/catalog         → catálogo de filtros
 *   GET    /internal/wardrobe/user/{userId}   → lista resumida (grid)
 *   GET    /internal/wardrobe/{clothingId}    → detalle compuesto (view/edit)
 *   PATCH  /internal/wardrobe/{clothingId}    → corrección manual
 *   DELETE /internal/wardrobe/{clothingId}    → eliminar prenda + imagen
 *
 * Nota: /catalog y /user/{userId} van ANTES de /{clothingId} para evitar
 * que Spring interprete "catalog" o "user" como un UUID.
 */
@RestController
@RequestMapping("/internal/wardrobe")
@RequiredArgsConstructor
@Slf4j
public class InternalWardrobeController {

    private final WardrobeOrchestratorService wardrobeService;

    // ── Upload ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingItemResponse> uploadClothing(
            @RequestPart("userId") String userIdStr,
            @RequestPart("image")  MultipartFile image) {

        UUID userId = UUID.fromString(userIdStr);
        log.info("Back-Wardrobe: POST /upload — usuario {}", userId);
        ClothingItemResponse response = wardrobeService.uploadClothing(
                new ClothingUploadRequest(userId), image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Catálogo (antes de /{clothingId} para evitar colisión de rutas) ───────

    @GetMapping("/catalog")
    public ResponseEntity<CatalogDTO> getCatalog() {
        log.info("Back-Wardrobe: GET /catalog");
        return ResponseEntity.ok(wardrobeService.getCatalog());
    }


    @GetMapping("/catalog/edit")
    public ResponseEntity<WardrobeEditCatalogDTO> getEditCatalog() {

        log.info("Back-Wardrobe: GET /catalog/edit");

        return ResponseEntity.ok(
                wardrobeService.getEditCatalog()
        );
    }

    // ── Lista resumida ────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClothingItemResponse>> getWardrobe(
            @PathVariable UUID userId) {
        log.info("Back-Wardrobe: GET /user/{}", userId);
        return ResponseEntity.ok(wardrobeService.getWardrobe(userId));
    }

    // ── Detalle compuesto ─────────────────────────────────────────────────────

    /**
     * GET /internal/wardrobe/{clothingId}
     *
     * Devuelve ClothingDetailResponse (prenda + audit completo).
     * Todos los campos del audit son null si isProcessed=false.
     */
    @GetMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> getClothingDetail(
            @PathVariable UUID clothingId) {
        log.info("Back-Wardrobe: GET /{}", clothingId);
        return ResponseEntity.ok(wardrobeService.getClothingDetail(clothingId));
    }

    // ── Corrección manual ─────────────────────────────────────────────────────

    /**
     * PATCH /internal/wardrobe/{clothingId}?userId={userId}
     *
     * El usuario corrige categoría, estilo, color, clima y ocasión.
     * Responde con el detalle actualizado (evita un GET adicional del frontend).
     */
    @PatchMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> updateClothing(
            @PathVariable UUID clothingId,
            @RequestParam  UUID userId,
            @Valid @RequestBody ClothingEditRequest request) {
        log.info("Back-Wardrobe: PATCH /{} — usuario {}", clothingId, userId);
        return ResponseEntity.ok(
                wardrobeService.updateClothing(clothingId, userId, request));
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    @DeleteMapping("/{clothingId}")
    public ResponseEntity<Void> deleteClothing(
            @PathVariable UUID clothingId,
            @RequestParam  UUID userId) {
        log.info("Back-Wardrobe: DELETE /{} — usuario {}", clothingId, userId);
        wardrobeService.deleteClothing(clothingId, userId);
        return ResponseEntity.noContent().build(); // 204
    }
}