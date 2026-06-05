package com.dressme.dressme_gateway.infra.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Punto único de manejo de excepciones del Gateway.
 *
 * Orden de los handlers: de más específico a más general.
 * Spring usa el handler más específico disponible para cada excepción.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Excepciones del dominio propio del Gateway ────────────────────────────

    @ExceptionHandler(InvalidOAuthTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOAuthToken(
        InvalidOAuthTokenException ex, HttpServletRequest request) {
        log.warn("Token OAuth inválido: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // ── Excepciones de la capa HTTP (RestClient, propagaciones del backend) ──

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
        ResponseStatusException ex, HttpServletRequest request) {
        // Las ResponseStatusException ya llevan el status correcto (401, 403, 502, etc.)
        log.debug("ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());
        return build(HttpStatus.resolve(ex.getStatusCode().value()), ex.getReason(), request);
    }

    // ── Validación de request body (@Valid) ───────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validación fallida: {}", message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // ── Argumentos inválidos (proveedor no soportado, etc.) ──────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
        IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(
        UnsupportedOperationException ex, HttpServletRequest request) {
        log.warn("Operación no soportada: {}", ex.getMessage());
        return build(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), request);
    }

    // ── Red de seguridad final ────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
        Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en el Gateway: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del Gateway.", request);
    }

    // ── Builder interno ───────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request) {
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(
                status.value(),
                message,
                request.getRequestURI(),
                Instant.now()
            ));
    }

    public record ErrorResponse(int status, String message, String path, Instant timestamp) {}
}
