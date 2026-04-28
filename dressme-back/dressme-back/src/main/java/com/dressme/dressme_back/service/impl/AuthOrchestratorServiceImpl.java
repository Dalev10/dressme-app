package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.service.AuthOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class AuthOrchestratorServiceImpl implements AuthOrchestratorService {

    private final RestClient restClient;

    public AuthOrchestratorServiceImpl(RestClient.Builder restClientBuilder) {
        // Asumimos que "dressme-database" es el nombre del contenedor en la red de Docker
        this.restClient = restClientBuilder.baseUrl("http://dressme-database:8080").build();
    }

    @Override
    public UserProfileResponse orchestrateLogin(StandardizedUserProviderInfo providerInfo) {
        
        // 1. Generar el Vector "Cold Start" para el MVP (1536 dimensiones en 0.0)
        float[] initialVector = new float[1536];
        Arrays.fill(initialVector, 0.0f);

        // 2. Construir el payload exacto que exige la base de datos
        InternalUserCreateRequest dbRequest = new InternalUserCreateRequest(
                providerInfo.email(),
                providerInfo.displayName(),
                providerInfo.profilePictureUrl(),
                providerInfo.provider(),
                providerInfo.providerId(),
                initialVector
        );

        // 3. Comunicación Interna: Enviar a dressme-database para que haga el proceso ACID
        // Si la base de datos falla (ej. caída de conexión), el RestClient lanzará una excepción
        // que nuestro manejador global de errores atrapará.
        return restClient.post()
                .uri("/internal/users/oauth-register")
                .body(dbRequest)
                .retrieve()
                .body(UserProfileResponse.class);
    }

    @Override
    public UserResponseDTO getUserProfile(UUID userId) {
        log.info("Back-Orquestador: Solicitando perfil a Database para ID: {}", userId);
        
        return restClient.get()
                .uri("/internal/users/{id}", userId)
                .retrieve()
                .body(UserResponseDTO.class);
    }
}