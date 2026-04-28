package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;

public interface AuthOrchestratorService {
    UserProfileResponse orchestrateLogin(StandardizedUserProviderInfo providerInfo);
}