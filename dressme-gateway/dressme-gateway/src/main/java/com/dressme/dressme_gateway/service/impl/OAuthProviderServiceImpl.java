package com.dressme.dressme_gateway.service.impl;

import com.dressme.dressme_gateway.infra.exception.InvalidOAuthTokenException;
import com.dressme.dressme_gateway.schema.dto.GoogleUserInfoResponse;
import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.service.OAuthProviderService;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OAuthProviderServiceImpl implements OAuthProviderService {

    private final RestClient restClient;

    // Inyectamos el builder moderno de Spring Boot 3
    public OAuthProviderServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public StandardizedUserProviderInfo validateAndExtractUserInfo(OAuthLoginRequest request) {
        return switch (request.provider().toUpperCase()) {
            case "GOOGLE" -> validateGoogleToken(request.token());
            case "PINTEREST" -> validatePinterestToken(request.token());
            default -> throw new IllegalArgumentException("Proveedor de autenticación no soportado: " + request.provider());
        };
    }

    private StandardizedUserProviderInfo validateGoogleToken(String idToken) {
        try {
            // Llamada síncrona a la API pública de validación de Google
            GoogleUserInfoResponse googleResponse = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (googleResponse == null || googleResponse.email() == null) {
                throw new InvalidOAuthTokenException("El token de Google no contiene información válida");
            }

            // Estandarización de la respuesta para el uso interno de DressMe
            return new StandardizedUserProviderInfo(
                    "GOOGLE",
                    googleResponse.providerId(),
                    googleResponse.email(),
                    googleResponse.displayName(),
                    googleResponse.profilePictureUrl()
            );

        } catch (RestClientResponseException e) {
            // Si Google responde con un 400 Bad Request, el token expiró o es falso
            throw new InvalidOAuthTokenException("Token de Google inválido, expirado o mal formado", e);
        }
    }

    private StandardizedUserProviderInfo validatePinterestToken(String accessToken) {
        // TODO: Implementar la validación con la API de Pinterest en una iteración futura.
        // Pinterest requiere un enfoque distinto enviando el Access Token en el Header de Autorización.
        throw new UnsupportedOperationException("La validación con Pinterest aún está en construcción");
    }
}