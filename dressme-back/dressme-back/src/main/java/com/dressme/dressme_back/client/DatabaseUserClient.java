package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_back.schema.dto.UserProfileResponse;
import com.dressme.dressme_back.schema.dto.UserResponseDTO;
import com.dressme.dressme_back.schema.dto.UserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "dressme-database-user-client", url = "${app.services.database-url}")
public interface DatabaseUserClient {

    @PostMapping("/internal/users/oauth-register")
    UserProfileResponse createOrFetchUser(@RequestBody InternalUserCreateRequest request);

    @GetMapping("/internal/users/{id}")
    UserResponseDTO getUserById(@PathVariable("id") UUID id);

    @PatchMapping("/internal/users/{id}")
    UserResponseDTO updateUser(@PathVariable("id") UUID id, @RequestBody UserUpdateRequest request);

    @DeleteMapping("/internal/users/{id}")
    void deleteUser(@PathVariable("id") UUID id);
}
