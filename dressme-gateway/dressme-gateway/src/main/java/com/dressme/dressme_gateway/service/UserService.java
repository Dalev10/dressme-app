package com.dressme.dressme_gateway.service;

import com.dressme.dressme_gateway.schema.dto.UserResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    private final RestClient restClient;

    public UserService(RestClient.Builder restClientBuilder) {
        // Apuntamos al nombre del servicio orquestador en la red de Docker
        this.restClient = restClientBuilder.baseUrl("http://dressme-back:8080").build();
    }

    public UserResponseDTO getUserProfile(UUID userId) {
        log.info("Gateway: Solicitando perfil completo al Orquestador para ID: {}", userId);
        
        return restClient.get()
                .uri("/internal/orchestrate/profile/{id}", userId)
                .retrieve()
                .body(UserResponseDTO.class);
    }
}