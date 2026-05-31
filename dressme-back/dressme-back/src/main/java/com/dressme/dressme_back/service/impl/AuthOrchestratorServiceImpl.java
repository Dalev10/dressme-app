package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.DatabaseUserClient;
import com.dressme.dressme_back.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import com.dressme.dressme_back.service.AuthOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthOrchestratorServiceImpl implements AuthOrchestratorService {

    private final DatabaseUserClient databaseUserClient;

    @Override
    public UserProfileResponse orchestrateLogin(StandardizedUserProviderInfo providerInfo) {
        log.info("Back-Orquestador: Orquestando login para provider={}, email={}",
            providerInfo.provider(), providerInfo.email());

        InternalUserCreateRequest dbRequest = new InternalUserCreateRequest(
            providerInfo.email(),
            providerInfo.displayName(),
            providerInfo.profilePictureUrl(),
            providerInfo.provider(),
            providerInfo.providerId(),
            new float[1536]
        );

        return databaseUserClient.createOrFetchUser(dbRequest);
    }

    @Override
    public UserResponseDTO getUserProfile(UUID userId) {
        log.info("Back-Orquestador: Solicitando perfil a Database para ID: {}", userId);
        return databaseUserClient.getUserById(userId);
    }

    @Override
    public UserResponseDTO updateProfile(UUID userId, UserUpdateRequest request) {
        log.info("Back-Orquestador: Solicitando actualización a Database para ID: {}", userId);
        return databaseUserClient.updateUser(userId, request);
    }

    @Override
    public void deleteProfile(UUID userId) {
        log.info("Back-Orquestador: Solicitando eliminación en Database para ID: {}", userId);
        databaseUserClient.deleteUser(userId);
    }
}
