package com.dressme.dressme_gateway.config;

import com.dressme.dressme_gateway.infra.exception.BackendErrorHandler;
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
     * reciba una instancia independiente. Esto evita el bug de estado compartido
     * cuando múltiples controladores llaman a .baseUrl() sobre el mismo builder singleton.
     *
     * El defaultStatusHandler centraliza el manejo de errores HTTP del backend
     * en un solo lugar para todos los RestClient del Gateway.
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .defaultStatusHandler(HttpStatusCode::isError, BackendErrorHandler::handle);
    }
}
