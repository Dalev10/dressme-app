package com.dressme.dressme_gateway.config;

import com.dressme.dressme_gateway.infra.exception.BackendErrorHandler;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        
        // 1. Configuración del cliente Netty con la sintaxis correcta
        HttpClient httpClient = HttpClient.create()
                // Timeout de conexión (Handshake) usando ChannelOption
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) 
                // Timeout de lectura (El tiempo de espera por la IA)
                .responseTimeout(Duration.ofSeconds(60)); 

        // 2. Factory correcta para inyectar Netty dentro del RestClient
        ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(httpClient);

        // 3. Construimos el builder
        return RestClient.builder()
            .requestFactory(factory)
            .defaultStatusHandler(HttpStatusCode::isError, BackendErrorHandler::handle);
    }

    @Bean
    public RestClient backClient(
            RestClient.Builder builder,
            @Value("${app.services.backend-url:http://dressme-back:8080}") String backendUrl
    ) {
        return builder.baseUrl(backendUrl).build();
    }
}