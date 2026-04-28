package com.dressme.dressme_database.service;

import java.util.UUID;

import com.dressme.dressme_database.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schema.dto.UserProfileResponse;
import com.dressme.dressme_database.schema.dto.UserResponseDTO;
import com.dressme.dressme_database.schema.dto.UserUpdateRequest;

public interface UserService {
    UserProfileResponse createUserFromOAuth(InternalUserCreateRequest request);
    UserResponseDTO getUserProfile(UUID userId);
    UserResponseDTO updateUserProfile(UUID userId, UserUpdateRequest request);
}