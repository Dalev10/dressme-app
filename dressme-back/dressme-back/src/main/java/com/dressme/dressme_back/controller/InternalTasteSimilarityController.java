package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_back.schema.dto.TasteSimilarityResponse;
import com.dressme.dressme_back.service.TasteSimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/taste-similarity")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Taste Similarity (Internal)", description = "Calcula la similitud entre el taste vector del usuario y outfits.")
public class InternalTasteSimilarityController {

    private final TasteSimilarityService tasteSimilarityService;

    @PostMapping("/batch")
    @Operation(summary = "Calcular similitud por batch", description = "Calcula la similitud coseno entre taste vector y outfit vector.")
    public ResponseEntity<List<TasteSimilarityResponse>> computeBatch(
        @Valid @RequestBody TasteSimilarityBatchRequest request) {
        log.info("Back-TasteSimilarity: scoring {} outfits for user {}", request.outfitIds().size(), request.userId());
        return ResponseEntity.ok(tasteSimilarityService.computeBatch(request));
    }
}
