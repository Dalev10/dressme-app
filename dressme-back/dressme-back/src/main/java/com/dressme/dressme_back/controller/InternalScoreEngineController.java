package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.schema.dto.ScoreEngineOrchestrationRequest;
import com.dressme.dressme_back.service.ScoreEngineService;
import com.dressme.dressme_back.service.ScoreEngineOrchestratorService;
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

@RestController
@RequestMapping("/internal/score")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Score Engine (Internal)", description = "Calcula el puntaje total de un outfit usando los componentes disponibles.")
public class InternalScoreEngineController {

    private final ScoreEngineService scoreEngineService;
    private final ScoreEngineOrchestratorService scoreEngineOrchestratorService;

    @PostMapping("/outfit")
    @Operation(summary = "Calcular score de outfit", description = "Compone el puntaje total (color, dresscode, gusto, tendencia) y re-normaliza si color no aplica.")
    public ResponseEntity<ScoreEngineResponse> scoreOutfit(@Valid @RequestBody ScoreEngineRequest request) {
        log.info("Back-ScoreEngine: scoring outfit");
        return ResponseEntity.ok(scoreEngineService.score(request));
    }

    @PostMapping("/outfit/context")
    @Operation(summary = "Calcular score de outfit con contexto", description = "Calcula el dresscode score con el usuario y el outfit antes de delegar al score engine.")
    public ResponseEntity<ScoreEngineResponse> scoreOutfitWithContext(
            @Valid @RequestBody ScoreEngineOrchestrationRequest request) {
        log.info("Back-ScoreEngine: scoring outfit con contexto userId={}, outfitId={}", request.userId(), request.outfitId());
        return ResponseEntity.ok(scoreEngineOrchestratorService.score(request));
    }
}
