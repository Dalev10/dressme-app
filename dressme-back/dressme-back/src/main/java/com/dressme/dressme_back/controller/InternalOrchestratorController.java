package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import com.dressme.dressme_back.service.AuthOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orchestrate")
@RequiredArgsConstructor
@Slf4j
public class InternalOrchestratorController {

    private final AuthOrchestratorService orchestratorService;

    /**
     * Endpoint receptor del Gateway. 
     * Aquí llega la información de identidad ya validada externamente.
     */
    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> handleLoginOrRegistration(
            @Valid @RequestBody StandardizedUserProviderInfo providerInfo) {
        
        // Disparamos la lógica de orquestación:
        // 1. Generación de vector (Mocked 1536 zeros)
        // 2. Llamada a dressme-database
        UserProfileResponse response = orchestratorService.orchestrateLogin(providerInfo);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserResponseDTO> getProfile(@PathVariable UUID id) {
        log.info("Back-Controller: Petición de perfil recibida para ID: {}", id);
        UserResponseDTO response = orchestratorService.getUserProfile(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable UUID id, 
            @RequestBody UserUpdateRequest request) {
        log.info("Back-Controller: Recibida petición de actualización para ID: {}", id);
        return ResponseEntity.ok(orchestratorService.updateProfile(id, request));
    }
}