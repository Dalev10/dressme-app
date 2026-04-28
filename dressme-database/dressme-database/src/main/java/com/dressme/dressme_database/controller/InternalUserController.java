package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schema.dto.UserProfileResponse;
import com.dressme.dressme_database.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
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
}