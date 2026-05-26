package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.ColorScoreRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.service.ColorScoreService;
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
public class InternalColorScoreController {

    private final ColorScoreService colorScoreService;

    @PostMapping
    public ResponseEntity<ColorScoreResponse> scoreColor(@Valid @RequestBody ColorScoreRequest request) {
        log.info("Back-ColorScore: scoring color for {} slots", request.items().size());
        return ResponseEntity.ok(colorScoreService.score(request));
    }
}
