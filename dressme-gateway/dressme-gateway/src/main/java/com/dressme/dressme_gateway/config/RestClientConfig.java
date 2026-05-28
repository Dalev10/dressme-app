package com.dressme.dressme_gateway.config;

import com.dressme.dressme_gateway.infra.exception.BackendErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Builder con scope PROTOTYPE para que cada componente que lo inyecte
     * reciba una instancia independiente.
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .defaultStatusHandler(HttpStatusCode::isError, BackendErrorHandler::handle);
    }

    /**
     * AQUÍ ESTÁ LA MAGIA QUE FALTABA:
     * Usamos el builder anterior para crear el RestClient final
     * inyectando la URL del backend.
     */
    @Bean
    public RestClient backClient(
            RestClient.Builder builder,
            @Value("${app.services.backend-url:http://dressme-back:8080}") String backendUrl
    ) {
        return builder.baseUrl(backendUrl).build();
    }
}