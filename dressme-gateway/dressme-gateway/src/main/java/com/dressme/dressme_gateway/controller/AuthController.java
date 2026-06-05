package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.AuthLoginResponse;
import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.schema.dto.UserResponseDTO;
import com.dressme.dressme_gateway.service.OAuthProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final OAuthProviderService oAuthProviderService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient backClient;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody OAuthLoginRequest request) {
        log.info("Gateway: Iniciando proceso de login con proveedor: {}", request.provider());

        StandardizedUserProviderInfo providerInfo = oAuthProviderService.validateAndExtractUserInfo(request);

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");
        UserResponseDTO userProfile = backClient.post()
                .uri("/internal/orchestrate/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(providerInfo)
                .retrieve()
                .body(UserResponseDTO.class);

        if (userProfile == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El backend no devolvió datos del usuario");
        }

        String jwtToken = jwtTokenProvider.generateToken(
                userProfile.getId(),
                userProfile.getEmail(),
                userProfile.getDisplayName()
        );

        AuthLoginResponse loginResponse = new AuthLoginResponse(
                userProfile.getId(),
                userProfile.getEmail(),
                userProfile.getDisplayName(),
                userProfile.getProfilePicture(),
                userProfile.isCalibrated(),
                jwtToken
        );

        log.info("Gateway: Login exitoso para usuario: {}", userProfile.getEmail());
        return ResponseEntity.ok(loginResponse);
    }
}
