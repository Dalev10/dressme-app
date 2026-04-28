package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schema.dto.UserProfileResponse;
import com.dressme.dressme_database.schema.dto.UserResponseDTO;
import com.dressme.dressme_database.schema.dto.UserUpdateRequest;
import com.dressme.dressme_database.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    /**
     * Endpoint interno para registrar o recuperar un usuario tras autenticarse vía OAuth.
     * Este endpoint es consumido exclusivamente por los microservicios orquestadores.
     */
    @PostMapping("/oauth-register")
    public ResponseEntity<UserProfileResponse> createOrFetchUser(
            @Valid @RequestBody InternalUserCreateRequest request) {
        
        UserProfileResponse response = userService.createUserFromOAuth(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        log.info("Database Microservice: Consultando perfil para usuario ID: {}", id);
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id, @RequestBody UserUpdateRequest request) {
        log.info("Database Controller: Recibida solicitud de actualización para usuario: {}", id);
        UserResponseDTO response = userService.updateUserProfile(id, request);
        return ResponseEntity.ok(response);
    }
}