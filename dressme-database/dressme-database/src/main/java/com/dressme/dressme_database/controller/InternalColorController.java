package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.ColorResponseDTO;
import com.dressme.dressme_database.service.ColorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/colors")
@RequiredArgsConstructor
@Slf4j
public class InternalColorController {

    private final ColorService colorService;

    @GetMapping("/{baseColorId}/complementary")
    public ResponseEntity<ColorResponseDTO> getComplementaryColor(@PathVariable UUID baseColorId) {
        log.info("Database: Solicitud de color complementario para el ID: {}", baseColorId);
        return ResponseEntity.ok(colorService.getComplementaryColor(baseColorId));
    }

    @GetMapping("/{baseColorId}/analogous")
    public ResponseEntity<List<ColorResponseDTO>> getAnalogousColors(@PathVariable UUID baseColorId) {
        log.info("Database: Solicitud de colores análogos para el ID: {}", baseColorId);
        return ResponseEntity.ok(colorService.getAnalogousColors(baseColorId));
    }
}
