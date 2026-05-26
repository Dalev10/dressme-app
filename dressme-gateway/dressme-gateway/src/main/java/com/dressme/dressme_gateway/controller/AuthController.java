package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.service.OAuthProviderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OAuthProviderService providerService;
    private final RestClient backClient;

    public AuthController(
            OAuthProviderService providerService,
            RestClient.Builder restClientBuilder,
            @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.providerService = providerService;
        // URL leída desde application.yml en lugar de hardcodeada
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody OAuthLoginRequest request) {

        StandardizedUserProviderInfo userInfo = providerService.validateAndExtractUserInfo(request);

        ResponseEntity<Object> backResponse = backClient.post()
                .uri("/internal/orchestrate/login")
                .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + request.token())
                .body(userInfo)
                .retrieve()
                .toEntity(Object.class);

        return ResponseEntity.status(backResponse.getStatusCode()).body(backResponse.getBody());
    }
}