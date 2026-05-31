package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.ColorScoreRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.service.ColorScoreService;
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
@RequestMapping("/internal/color-score")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Color Score (Internal)", description = "Calcula el score de color por slots y devuelve pares ponderados.")
public class InternalColorScoreController {

    private final ColorScoreService colorScoreService;

    @PostMapping
    @Operation(summary = "Calcular score de color", description = "Calcula la compatibilidad cromatica por slots y define si aplica al outfit.")
    public ResponseEntity<ColorScoreResponse> scoreColor(@Valid @RequestBody ColorScoreRequest request) {
        log.info("Back-ColorScore: scoring color for {} slots", request.items().size());
        return ResponseEntity.ok(colorScoreService.score(request));
    }
}
