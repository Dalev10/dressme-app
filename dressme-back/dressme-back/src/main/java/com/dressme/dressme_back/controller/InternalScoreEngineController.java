package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.service.ScoreEngineService;
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

    @PostMapping("/outfit")
    @Operation(summary = "Calcular score de outfit", description = "Compone el puntaje total (color, dresscode, gusto, tendencia) y re-normaliza si color no aplica.")
    public ResponseEntity<ScoreEngineResponse> scoreOutfit(@Valid @RequestBody ScoreEngineRequest request) {
        log.info("Back-ScoreEngine: scoring outfit");
        return ResponseEntity.ok(scoreEngineService.score(request));
    }
}
