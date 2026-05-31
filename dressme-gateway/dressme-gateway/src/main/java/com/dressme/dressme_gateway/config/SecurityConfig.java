package com.dressme.dressme_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("********** GATEWAY: CARGANDO CONFIGURACIÓN DE SEGURIDAD PERSONALIZADA **********");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Swagger / OpenAPI ─────────────────────────────────────────
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()

                // ── Auth ──────────────────────────────────────────────────────
                .requestMatchers("/api/v1/auth/login").permitAll()

                // ── Usuarios ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/v1/users/profile/**").permitAll()
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/profile/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/profile/**").permitAll()

                // ── Onboarding ────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/v1/onboarding/style-cards").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/onboarding/calibrate").permitAll()


                // ── Wardrobe ─────────────────────────────────────────────

                .requestMatchers(HttpMethod.POST,
                        "/api/v1/wardrobe/upload")
                .permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/wardrobe/catalog")
                .permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/wardrobe/catalog/edit")
                .permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/wardrobe/list")
                .permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/wardrobe/*")
                .permitAll()

                .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/wardrobe/*")
                .permitAll()

                .requestMatchers(HttpMethod.DELETE,
                        "/api/v1/wardrobe/*")
                .permitAll()

                // ── Protegidas ────────────────────────────────────────────────
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}