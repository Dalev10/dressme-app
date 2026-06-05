package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos de Wardrobe (guardarropa).
 * Recibe peticiones del frontend y las proxea a dressme-back.
 *
 * Rutas:
 *   POST   /api/v1/wardrobe/upload              → subir prenda (multipart)
 *   GET    /api/v1/wardrobe/catalog             → catálogo de filtros
 *   GET    /api/v1/wardrobe/list?userId={uuid}  → lista resumida (grid)
 *   GET    /api/v1/wardrobe/{clothingId}        → detalle compuesto (view/edit)
 *   PATCH  /api/v1/wardrobe/{clothingId}        → corrección manual
 *   DELETE /api/v1/wardrobe/{clothingId}        → eliminar prenda + imagen
 */
@RestController
@RequestMapping("/api/v1/wardrobe")
@Slf4j
public class WardrobeController {

    private final RestClient backClient;
    private final JwtTokenProvider jwtTokenProvider;

    public WardrobeController(
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.services.backend-url}") String backendUrl
            
    ) {
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * POST /api/v1/wardrobe/upload
     *
     * Proxea la carga de imágenes a dressme-back vía multipart/form-data.
     * Requiere:
     *   - userId (form part): UUID del propietario de la prenda
     *   - image (form part): archivo de imagen (JPEG/PNG)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingItemResponse> uploadClothing(
            @RequestPart("userId") String userIdStr,
            @RequestPart("image") MultipartFile image) {

        UUID userId = UUID.fromString(userIdStr);
        log.info("Gateway-Wardrobe: POST /upload — usuario {}", userId);

        // Construir el multipart body para proxear al backend
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("userId", userId.toString());
        body.add("image", image.getResource());

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        ClothingItemResponse response = backClient.post()
                .uri("/internal/wardrobe/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(body)
                .retrieve()
                .body(ClothingItemResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/wardrobe/catalog
     *
     * Devuelve el catálogo completo de filtros:
     * categorías, ocasiones, climas.
     */
    @GetMapping("/catalog")
    public ResponseEntity<CatalogDTO> getCatalog() {
        log.info("Gateway-Wardrobe: GET /catalog");

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        CatalogDTO catalog = backClient.get()
                .uri("/internal/wardrobe/catalog")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(CatalogDTO.class);

        return ResponseEntity.ok(catalog);
    }


    @GetMapping("/catalog/edit")
    public ResponseEntity<WardrobeEditCatalogDTO> getEditCatalog() {

        log.info("Gateway-Wardrobe: GET /catalog/edit");

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        WardrobeEditCatalogDTO catalog = backClient.get()
                .uri("/internal/wardrobe/catalog/edit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(WardrobeEditCatalogDTO.class);

        return ResponseEntity.ok(catalog);
   }




    // ── Lista resumida ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/wardrobe/list?userId={userId}
     *
     * Devuelve lista resumida de prendas del usuario para el grid.
     */
    @GetMapping("/list")
    public ResponseEntity<List<ClothingItemResponse>> getWardrobe(
            @RequestParam UUID userId) {

        log.info("Gateway-Wardrobe: GET /list para usuario {}", userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        List<ClothingItemResponse> items = backClient.get()
                .uri("/internal/wardrobe/user/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ClothingItemResponse>>() {});

        return ResponseEntity.ok(items);
    }

    // ── Detalle compuesto ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/wardrobe/{clothingId}
     *
     * Devuelve ClothingDetailResponse (prenda + audit completo).
     * Todos los campos del audit son null si isProcessed=false.
     */
    @GetMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> getClothingDetail(
            @PathVariable UUID clothingId) {

        log.info("Gateway-Wardrobe: GET /{}", clothingId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        ClothingDetailResponse detail = backClient.get()
                .uri("/internal/wardrobe/{clothingId}", clothingId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .body(ClothingDetailResponse.class);

        return ResponseEntity.ok(detail);
    }

    // ── Corrección manual ─────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/wardrobe/{clothingId}?userId={userId}
     *
     * El usuario corrige categoría, estilo, color, clima y ocasión.
     * Devuelve el detalle actualizado (evita un GET adicional del frontend).
     */
    @PatchMapping("/{clothingId}")
    public ResponseEntity<ClothingDetailResponse> updateClothing(
            @PathVariable UUID clothingId,
            @RequestParam UUID userId,
            @Valid @RequestBody ClothingEditRequest request) {

        log.info("Gateway-Wardrobe: PATCH /{} — usuario {}", clothingId, userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        ClothingDetailResponse updated = backClient.patch()
                .uri("/internal/wardrobe/{clothingId}?userId={userId}",
                        clothingId, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(request)
                .retrieve()
                .body(ClothingDetailResponse.class);

        return ResponseEntity.ok(updated);
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/wardrobe/{clothingId}?userId={userId}
     *
     * Elimina la prenda y su imagen del almacenamiento.
     */
    @DeleteMapping("/{clothingId}")
    public ResponseEntity<Void> deleteClothing(
            @PathVariable UUID clothingId,
            @RequestParam UUID userId) {

        log.info("Gateway-Wardrobe: DELETE /{} — usuario {}", clothingId, userId);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        backClient.delete()
                .uri("/internal/wardrobe/{clothingId}?userId={userId}",
                        clothingId, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .toBodilessEntity();

        return ResponseEntity.noContent().build(); // 204
    }
}
