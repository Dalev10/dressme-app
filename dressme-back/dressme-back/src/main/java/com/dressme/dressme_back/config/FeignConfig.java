package com.dressme.dressme_back.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public Client feignClient() {
        log.info("FeignConfig: Initializing Feign OkHttpClient (supports PATCH method)");
        okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(70, TimeUnit.SECONDS)
                .readTimeout(70, TimeUnit.SECONDS)
                .writeTimeout(70, TimeUnit.SECONDS)
                .build();
        return new OkHttpClient(httpClient);
    }
}