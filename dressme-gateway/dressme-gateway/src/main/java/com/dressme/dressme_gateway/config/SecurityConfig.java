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
            // 1. Deshabilitar CSRF: Esencial para APIs REST que no usan cookies de sesión,
            // evitando que Spring Security bloquee los POST/PUT.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configurar CORS: Permite que el Frontend (en otro puerto/dominio) 
            // pueda consumir los recursos del Gateway.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Política Stateless: El Gateway no guardará estado de sesión en el servidor.
            // Cada petición es independiente.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 4. Reglas de Autorización: Definimos quién puede entrar a qué rutas.
            .authorizeHttpRequests(auth -> auth
                // --- RUTAS PÚBLICAS (Lista Blanca) ---
                
                // Acceso total a la documentación de Swagger y OpenAPI
                .requestMatchers(
                        "/v3/api-docs/**", 
                        "/swagger-ui/**", 
                        "/swagger-ui.html"
                ).permitAll()
                
                // Permitir el flujo de Login con Google
                .requestMatchers("/api/v1/auth/login").permitAll()
                
                // INTEGRIDAD: Permitir el acceso al perfil de usuario para pruebas del MVP.
                // Esta es la línea que permite que el puerto 8080 responda tu consulta.
                .requestMatchers("/api/v1/users/profile/**").permitAll() 
                
                // --- RUTAS PROTEGIDAS ---
                
                // Cualquier otra petición que no esté arriba requerirá autenticación.
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Configuración de CORS (Cross-Origin Resource Sharing).
     * Define las reglas de interacción con orígenes externos.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Durante el desarrollo MVP permitimos todos los orígenes ("*").
        // En producción se limitaría a "https://midominio.com".
        configuration.setAllowedOrigins(List.of("*")); 
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Cabeceras permitidas para el envío de Tokens (Authorization) y JSON
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}