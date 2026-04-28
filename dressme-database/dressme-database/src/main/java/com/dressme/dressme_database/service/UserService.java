package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schema.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse createUserFromOAuth(InternalUserCreateRequest request);
}