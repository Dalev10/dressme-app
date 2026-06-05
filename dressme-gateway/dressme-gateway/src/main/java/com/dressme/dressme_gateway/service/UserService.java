package com.dressme.dressme_gateway.service;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.UserResponseDTO;
import com.dressme.dressme_gateway.schema.dto.UserUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@Slf4j
public class UserService {

    private final RestClient restClient;
    private final JwtTokenProvider jwtTokenProvider; // <- 1. Declarar provider

    public UserService(
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider, // <- 2. Inyectar provider
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(backendUrl).build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserResponseDTO getUserProfile(UUID userId) {
        log.info("Gateway: Solicitando perfil completo al Orquestador para ID: {}", userId);
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway"); // <- 3. Generar token

        return restClient.get()
                .uri("/internal/orchestrate/profile/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken) // <- 4. Adjuntar header
                .retrieve()
                .body(UserResponseDTO.class);
    }

    public UserResponseDTO updateUserProfile(UUID userId, UserUpdateRequest request) {
        log.info("Gateway-Service: Enviando actualización al Orquestador para ID: {}", userId);
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        return restClient.patch()
                .uri("/internal/orchestrate/profile/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(request)
                .retrieve()
                .body(UserResponseDTO.class);
    }

    public void deleteUserProfile(UUID userId) {
        log.info("Gateway-Service: Comunicando baja de usuario al Back para ID: {}", userId);
        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        restClient.delete()
                .uri("/internal/orchestrate/profile/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .retrieve()
                .toBodilessEntity();
    }
}