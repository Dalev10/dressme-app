package com.dressme.dressme_gateway.service;

import com.dressme.dressme_gateway.schemas.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schemas.dto.StandardizedUserProviderInfo;

public interface OAuthProviderService {
    StandardizedUserProviderInfo validateAndExtractUserInfo(OAuthLoginRequest request);
}