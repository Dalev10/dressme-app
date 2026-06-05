package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.DatabaseUserClient;
import com.dressme.dressme_back.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import com.dressme.dressme_back.service.AuthOrchestratorService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthOrchestratorServiceImpl implements AuthOrchestratorService {

    private static final int MAX_RETRIES = 5;
    private static final long[] RETRY_DELAYS_MS = {5_000, 10_000, 20_000, 30_000, 45_000};

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

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return databaseUserClient.createOrFetchUser(dbRequest);
            } catch (FeignException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    log.error("Back-Orquestador: dressme-database sigue con 429 tras {} intentos", MAX_RETRIES + 1);
                    throw e;
                }
                long delay = RETRY_DELAYS_MS[attempt];
                log.warn("Back-Orquestador: 429 de dressme-database (cold start), reintentando en {}ms (intento {}/{})",
                        delay, attempt + 1, MAX_RETRIES);
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
            }
        }
        throw new IllegalStateException("unreachable");
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
