package com.dressme.dressme_back.service;

import java.util.UUID;

import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;

public interface AuthOrchestratorService {
    UserProfileResponse orchestrateLogin(StandardizedUserProviderInfo providerInfo);
    UserResponseDTO getUserProfile(UUID userId);
}