package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_database.schema.dto.StyleCardDTO;
import com.dressme.dressme_database.schema.dto.StyleCardEmbeddingResponse;
import com.dressme.dressme_database.service.StyleCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos de onboarding.
 * Solo accesibles desde la red interna Docker (dressme-back).
 * El prefijo /internal/ los deja fuera del alcance público del Gateway.
 */
@RestController
@RequestMapping("/internal/onboarding")
@RequiredArgsConstructor
@Slf4j
public class InternalStyleCardController {

    private final StyleCardService styleCardService;

    /**
     * GET /internal/onboarding/style-cards
     *
     * Devuelve las tarjetas activas para la pantalla de onboarding.
     * dressme-back las proxeará al Gateway → Frontend.
     */
    @GetMapping("/style-cards")
    public ResponseEntity<List<StyleCardDTO>> getStyleCards() {
        log.info("Database: Solicitud de tarjetas de onboarding");
        return ResponseEntity.ok(styleCardService.getActiveStyleCards());
    }

    /**
     * POST /internal/onboarding/style-cards/embeddings
     * Body: ["uuid1", "uuid2", ...]
     *
     * Retorna los embeddings de las tarjetas seleccionadas.
     * Consumido exclusivamente por dressme-back durante el flujo de calibración.
     */
    @PostMapping("/style-cards/embeddings")
    public ResponseEntity<List<StyleCardEmbeddingResponse>> getEmbeddings(
            @RequestBody List<UUID> styleCardIds) {
        log.info("Database: Solicitud de embeddings para {} tarjetas", styleCardIds.size());
        return ResponseEntity.ok(styleCardService.getEmbeddingsByIds(styleCardIds));
    }

    /**
     * POST /internal/onboarding/selections
     *
     * Persiste las reacciones del usuario a las tarjetas.
     * Soporta re-calibración: reemplaza selecciones previas del mismo usuario.
     */
    @PostMapping("/selections")
    public ResponseEntity<Void> saveSelections(
            @Valid @RequestBody OnboardingSelectionRequest request) {
        log.info("Database: Guardando selecciones de onboarding para usuario {}", request.userId());
        styleCardService.saveSelections(request);
        return ResponseEntity.noContent().build(); // 204
    }
}