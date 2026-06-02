package com.dressme.dressme_back.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public Client feignClient() {
        log.info("FeignConfig: Initializing Feign OkHttpClient (supports PATCH method)");
        return new OkHttpClient(new okhttp3.OkHttpClient());
    }
}