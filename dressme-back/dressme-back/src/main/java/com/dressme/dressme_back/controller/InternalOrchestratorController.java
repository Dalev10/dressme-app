package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.service.AuthOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orchestrate")
@RequiredArgsConstructor
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
}