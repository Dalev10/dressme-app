package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_gateway.schema.dto.TasteSimilarityResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taste-similarity")
@Slf4j
public class TasteSimilarityController {

    private final RestClient backClient;

    public TasteSimilarityController(
        RestClient.Builder restClientBuilder,
        @Value("${app.services.backend-url}") String backendUrl
    ) {
        this.backClient = restClientBuilder.baseUrl(backendUrl).build();
    }

    @PostMapping("/batch")
    public ResponseEntity<List<TasteSimilarityResponse>> computeBatch(
        @Valid @RequestBody TasteSimilarityBatchRequest request,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Gateway-TasteSimilarity: POST /batch for user {}", request.userId());
        requireBearerToken(authHeader);

        List<TasteSimilarityResponse> responses = backClient.post()
            .uri("/internal/taste-similarity/batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(responses);
    }

    private void requireBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway-TasteSimilarity: Missing Authorization Bearer header");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Se requiere un token Bearer en el header Authorization."
            );
        }
    }
}
