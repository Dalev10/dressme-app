package com.dressme.dressme_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Deshabilitar CSRF (Crucial para APIs RESTful y microservicios)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configurar CORS (Para que React/Mobile puedan hacer peticiones sin ser bloqueados)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Política Stateless (El Gateway no guarda sesiones en memoria, usaremos Tokens)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 4. Reglas de Autorización (El filtro principal)
            .authorizeHttpRequests(auth -> auth
                // --- RUTAS PÚBLICAS (Lista Blanca) ---
                // Permitir acceso total a la interfaz y JSONs de Swagger
                .requestMatchers(
                        "/v3/api-docs/**", 
                        "/swagger-ui/**", 
                        "/swagger-ui.html"
                ).permitAll()
                
                // Permitir acceso al endpoint de Login que creamos anteriormente
                .requestMatchers("/api/v1/auth/login").permitAll()
                
                // --- RUTAS PROTEGIDAS ---
                // Cualquier otra petición futura deberá tener un Token válido
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Configuración de CORS (Cross-Origin Resource Sharing)
     * Define qué dominios (frontends) tienen permiso para hablar con este Gateway.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Para desarrollo local, permitimos cualquier origen temporalmente. 
        // En producción aquí iría "https://midominio.com"
        configuration.setAllowedOrigins(List.of("*")); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}