package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.AuthLoginResponse;
import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.service.OAuthProviderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final OAuthProviderService providerService;
    private final RestClient backClient;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            OAuthProviderService providerService,
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.providerService = providerService;
        this.jwtTokenProvider = jwtTokenProvider;
        // URL leída desde application.yml en lugar de hardcodeada
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> authenticateUser(@Valid @RequestBody OAuthLoginRequest request) {
        // 1. Validar token OAuth contra el proveedor (Google)
        StandardizedUserProviderInfo userInfo = providerService.validateAndExtractUserInfo(request);
        log.info("Gateway-Auth: Token OAuth validado para usuario: {}", userInfo.email());

        // 2. Generar el token interno M2M para autorizar la petición al Backend
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        // 3. Orquestar creación/recuperación de usuario en el backend usando el token interno
        ResponseEntity<Object> backResponse = backClient.post()
            .uri("/internal/orchestrate/login")
            .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
            .body(userInfo)
            .retrieve()
            .toEntity(Object.class);

        if (!backResponse.getStatusCode().is2xxSuccessful()) {
            log.warn("Backend respondió con status: {}", backResponse.getStatusCode());
            return ResponseEntity.status(backResponse.getStatusCode()).build();
        }

        // Archivo: dressme-gateway/src/main/java/com/dressme/dressme_gateway/controller/AuthController.java

        // 4. El backend devuelve un UserProfileResponse con el id del usuario
        @SuppressWarnings("unchecked")
        var responseBody = (java.util.Map<String, Object>) backResponse.getBody();

        if (responseBody == null || !responseBody.containsKey("id")) {
            log.error("Backend no devolvió una respuesta válida con id");
            return ResponseEntity.internalServerError().build();
        }

        UUID userId = UUID.fromString((String) responseBody.get("id"));
        
        // Extraemos el valor real de calibración enviado por el backend
        boolean isCalibrated = responseBody.containsKey("isCalibrated") 
                ? (Boolean) responseBody.get("isCalibrated") 
                : false;

        // 5. Generar JWT para el cliente (frontend)
        String jwtToken = jwtTokenProvider.generateToken(userId, userInfo.email(), userInfo.displayName());
        log.info("Gateway-Auth: JWT generado para usuario: {}", userInfo.email());

        // 6. Construir respuesta con token
        AuthLoginResponse authResponse = new AuthLoginResponse(
            userId,
            userInfo.email(),
            userInfo.displayName(),
            userInfo.profilePictureUrl(),
            isCalibrated, // <-- Usar el valor dinámico 
            jwtToken
        );

        return ResponseEntity.ok(authResponse);
    }
}
