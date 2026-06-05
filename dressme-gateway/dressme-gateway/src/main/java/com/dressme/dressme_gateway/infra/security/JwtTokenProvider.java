package com.dressme.dressme_gateway.infra.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Genera y valida JWT para sesiones de usuario post-login.
 * Utilizado por el Gateway para emitir tokens tras validar OAuth.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationHours;

    public JwtTokenProvider(
        @Value("${app.security.jwt-secret:dressme-secret-key-change-in-production-minimum-256-bits-required-12345}") String jwtSecret,
        @Value("${app.security.jwt-expiration-hours:24}") long expirationHours
    ) {
        // Asegurar que tenga al menos 256 bits (32 bytes)
        if (jwtSecret.length() < 32) {
            log.warn("JWT secret es muy corto ({}), usando default extendido", jwtSecret.length());
            jwtSecret = "dressme-secret-key-change-in-production-minimum-256-bits-required-12345";
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
        log.info("JwtTokenProvider inicializado con duración: {} horas", expirationHours);
    }

    /**
     * Genera un JWT para un usuario autenticado.
     *
     * @param userId UUID del usuario
     * @param email Email del usuario (claim)
     * @param displayName Nombre del usuario (claim)
     * @return Token JWT válido
     */
    public String generateToken(UUID userId, String email, String displayName) {
        Instant now = Instant.now();
        Instant expiryTime = now.plus(expirationHours, ChronoUnit.HOURS);

        String token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("displayName", displayName)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryTime))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();

        log.debug("Token JWT generado para usuario: {}", email);
        return token;
    }

    public String generateInternalServiceToken(String serviceName) {
        Instant now = Instant.now();
        // Los tokens internos suelen tener vidas muy cortas (ej. 5 minutos)
        Instant expiryTime = now.plus(5, ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(serviceName)
            .claim("role", "INTERNAL_SERVICE")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryTime))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Extrae el ID del usuario (subject) de un token generado por este mismo provider.
     */
    public String getUserIdFromToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        } catch (Exception e) {
            log.error("Error extrayendo userId del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido o expirado");
        }
    }
}
