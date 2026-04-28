package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schemas.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schemas.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.service.OAuthProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthProviderService providerService;
    // Usamos RestClient para comunicarnos internamente con nuestro ecosistema
    private final RestClient restClient; 

    /**
     * Endpoint PÚBLICO: El Frontend envía el token de Google/Pinterest aquí.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody OAuthLoginRequest request) {
        
        // 1. Validar el token contra los servidores externos (Google/Pinterest)
        // Si el token es falso o expiró, esto lanzará InvalidOAuthTokenException (HTTP 401)
        StandardizedUserProviderInfo userInfo = providerService.validateAndExtractUserInfo(request);

        // 2. Comunicación Interna: Enviar la data validada a dressme-back (El Orquestador)
        // Nota Arquitectónica: Aquí el Gateway actúa como Proxy Reverso con lógica.
        // Asumimos que dressme-back está corriendo en la red interna de Docker (ej. http://dressme-back:8080)
        
        ResponseEntity<Object> backResponse = restClient.post()
                .uri("http://dressme-back:8080/internal/orchestrate/login") // URI de la red interna
                .body(userInfo)
                .retrieve()
                // .toEntity(Object.class) nos permite devolver al frontend exactamente 
                // lo que el orquestador decida (ej. un JWT propio de nuestra app, datos del perfil, etc.)
                .toEntity(Object.class); 

        return ResponseEntity.status(backResponse.getStatusCode()).body(backResponse.getBody());
    }
}