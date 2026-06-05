package com.dressme.dressme_gateway.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Valida el JWT de usuario en cada request.
 * Si el token es válido, registra el authentication en el SecurityContext.
 * Si falta o es inválido, el contexto queda vacío y Spring Security
 * rechaza la ruta con 401 si está protegida.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(
            @Value("${app.security.jwt-secret:dressme-secret-key-change-in-production-minimum-256-bits-required-12345}") String jwtSecret
    ) {
        if (jwtSecret.length() < 32) {
            jwtSecret = "dressme-secret-key-change-in-production-minimum-256-bits-required-12345";
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String role    = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities = "INTERNAL_SERVICE".equals(role)
                    ? List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(subject, null, authorities)
            );

            log.debug("JWT validado — subject: {}", subject);

        } catch (Exception e) {
            log.warn("JWT inválido o expirado: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
