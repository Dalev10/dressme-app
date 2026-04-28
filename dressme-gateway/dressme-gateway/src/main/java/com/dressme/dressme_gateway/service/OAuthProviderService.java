package com.dressme.dressme_gateway.service;

import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;

public interface OAuthProviderService {
    StandardizedUserProviderInfo validateAndExtractUserInfo(OAuthLoginRequest request);
}