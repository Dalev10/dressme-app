package com.dressme.dressme_gateway.service.impl;

import com.dressme.dressme_gateway.infra.exception.InvalidOAuthTokenException;
import com.dressme.dressme_gateway.schema.dto.GoogleUserInfoResponse;
import com.dressme.dressme_gateway.schema.dto.OAuthLoginRequest;
import com.dressme.dressme_gateway.schema.dto.StandardizedUserProviderInfo;
import com.dressme.dressme_gateway.service.OAuthProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
public class OAuthProviderServiceImpl implements OAuthProviderService {

    private static final String GOOGLE_TOKENINFO_URL =
        "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final RestClient restClient;

    public OAuthProviderServiceImpl(RestClient.Builder restClientBuilder) {
        // Cliente sin error handler global: llamadas a APIs externas (Google),
        // no al backend interno. Los errores de Google se manejan aquí explícitamente.
        this.restClient = RestClient.builder().build();
    }

    @Override
    public StandardizedUserProviderInfo validateAndExtractUserInfo(OAuthLoginRequest request) {
        return switch (request.provider().toUpperCase()) {
            case "GOOGLE"    -> validateGoogleToken(request.token());
            case "PINTEREST" -> validatePinterestToken(request.token());
            default -> throw new IllegalArgumentException(
                "Proveedor de autenticación no soportado: " + request.provider()
            );
        };
    }

    private StandardizedUserProviderInfo validateGoogleToken(String idToken) {
        log.info("Iniciando validación de token con Google OAuth2...");

        try {
            GoogleUserInfoResponse googleResponse = restClient.get()
                .uri(GOOGLE_TOKENINFO_URL, idToken)
                .retrieve()
                .body(GoogleUserInfoResponse.class);

            if (googleResponse == null || googleResponse.email() == null) {
                throw new InvalidOAuthTokenException(
                    "El token de Google no contiene información de usuario válida"
                );
            }

            log.info("Token de Google validado correctamente para el usuario: {}",
                googleResponse.email());

            return new StandardizedUserProviderInfo(
                "GOOGLE",
                googleResponse.providerId(),
                googleResponse.email(),
                googleResponse.displayName(),
                googleResponse.profilePictureUrl()
            );

        } catch (RestClientResponseException e) {
            log.warn("Google rechazó el token. Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new InvalidOAuthTokenException(
                "Token de Google inválido, expirado o mal formado", e
            );
        }
    }

    private StandardizedUserProviderInfo validatePinterestToken(String accessToken) {
        // TODO: Implementar en una iteración futura.
        // Pinterest requiere el Access Token en el header Authorization de la petición.
        throw new UnsupportedOperationException(
            "La validación con Pinterest aún está en construcción"
        );
    }
}
