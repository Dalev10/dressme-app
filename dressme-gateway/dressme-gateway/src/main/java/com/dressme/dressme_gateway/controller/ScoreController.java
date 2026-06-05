package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.infra.security.JwtTokenProvider;
import com.dressme.dressme_gateway.schema.dto.ScoreRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/score")
@Slf4j
public class ScoreController {

    private final RestClient       backClient;
    private final JwtTokenProvider jwtTokenProvider;

    public ScoreController(
            RestClient.Builder restClientBuilder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.services.backend-url:http://dressme-back:8080}") String backendUrl
    ) {
        this.backClient       = restClientBuilder.baseUrl(backendUrl).build();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/outfit")
    public ResponseEntity<Object> scoreOutfit(
            @Valid @RequestBody ScoreRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        requireBearerToken(authHeader);

        String userToken = authHeader.substring(7);
        UUID   userId    = UUID.fromString(jwtTokenProvider.getUserIdFromToken(userToken));

        log.info("Gateway-Score: POST /outfit userId={} outfitId={}", userId, request.outfitId());

        String internalToken = jwtTokenProvider.generateInternalServiceToken("dressme-gateway");

        Map<String, Object> backRequest = new HashMap<>();
        backRequest.put("userId",           userId);
        backRequest.put("outfitId",         request.outfitId());
        backRequest.put("color",            request.color());
        backRequest.put("tasteScore",       request.tasteScore());
        backRequest.put("outfitEmbeddings", request.outfitEmbeddings());

        Object response = backClient.post()
                .uri("/internal/score/outfit/context")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .body(backRequest)
                .retrieve()
                .body(Object.class);

        return ResponseEntity.ok(response);
    }

    private void requireBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway-Score: Petición rechazada — falta Authorization Bearer");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Se requiere un token Bearer en el header Authorization."
            );
        }
    }
}
