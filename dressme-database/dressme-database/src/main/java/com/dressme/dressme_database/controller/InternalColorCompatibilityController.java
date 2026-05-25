package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.facade.ColorOrchestratorFacade;
import com.dressme.dressme_database.schema.dto.ColorCompatibilityRequestDTO;
import com.dressme.dressme_database.schema.dto.CompatibilityResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/colors")
@RequiredArgsConstructor
@Validated
@Slf4j
public class InternalColorCompatibilityController {

    private final ColorOrchestratorFacade  colorOrchestratorFacade;

    @GetMapping("/compatibility")
    public ResponseEntity<CompatibilityResponseDTO> checkCompatibility(
        @Valid @ModelAttribute ColorCompatibilityRequestDTO request){

        log.info("Database: Solicitud de compatibilidad entre colores HSL({},{},{}) y HSL({},{},{})",
            request.hue1(), request.saturation1(), request.lightness1(),
            request.hue2(), request.saturation2(), request.lightness2());

        CompatibilityResponseDTO response = colorOrchestratorFacade.checkCompatibility(
            request.hue1(), request.saturation1(), request.lightness1(),
            request.hue2(), request.saturation2(), request.lightness2()
        );

        return ResponseEntity.ok(response);    }
}
