package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schemas.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schemas.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse createUserFromOAuth(InternalUserCreateRequest request);
}