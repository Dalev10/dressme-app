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
            log.warn("Error del cliente en llamada al backend ({}). URI: {}", status, uri);
            throw new ResponseStatusException(
                status,
                "Error en la petición al servicio backend."
            );
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
