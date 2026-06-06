package com.dressme.dressme_gateway.infra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Manejador centralizado de errores HTTP para todas las llamadas al backend interno.
 *
 * Se registra como defaultStatusHandler en RestClientConfig para que todos los
 * RestClient del Gateway lo apliquen automáticamente, sin duplicar lógica en
 * cada controlador.
 */
@Slf4j
public final class BackendErrorHandler {

    private BackendErrorHandler() {}

    private static String extractMessage(String body) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            if (node.has("message")) return node.get("message").asText();
        } catch (Exception ignored) {}
        return body.isBlank() ? "Error en la petición al servicio backend." : body;
    }

    public static void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String uri = request.getURI().toString();

        if (status == HttpStatus.UNAUTHORIZED) {
            log.warn("Backend rechazó la petición con 401. URI: {}", uri);
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token inválido o expirado. Por favor renueva tu sesión."
            );
        }

        if (status == HttpStatus.FORBIDDEN) {
            log.warn("Acceso denegado por el backend (403). URI: {}", uri);
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta acción."
            );
        }

        if (status.is4xxClientError()) {
            String body = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            log.warn("Error del cliente en llamada al backend ({}). URI: {} — body: {}", status, uri, body);
            String message = extractMessage(body);
            throw new ResponseStatusException(status, message);
        }

        if (status.is5xxServerError()) {
            log.error("Error interno del backend ({}). URI: {}", status, uri);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "El servicio backend no está disponible temporalmente."
            );
        }
    }
}
