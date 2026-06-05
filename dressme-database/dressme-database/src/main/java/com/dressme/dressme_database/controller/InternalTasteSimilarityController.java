package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.TasteSimilarityBatchRequestDTO;
import com.dressme.dressme_database.schema.dto.TasteSimilarityResponseDTO;
import com.dressme.dressme_database.service.TasteSimilarityService;
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
public class InternalTasteSimilarityController {

    private final TasteSimilarityService tasteSimilarityService;

    @PostMapping("/batch")
    public ResponseEntity<List<TasteSimilarityResponseDTO>> computeBatch(
        @Valid @RequestBody TasteSimilarityBatchRequestDTO request) {
        log.info("Database: Taste similarity batch for user {} with {} outfits",
            request.userId(), request.outfitIds().size());
        return ResponseEntity.ok(tasteSimilarityService.computeBatch(request));
    }
}
