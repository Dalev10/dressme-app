package com.dressme.dressme_back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter 
@Setter
public class AppConfig {
    
    private final Services services = new Services();

    @Getter 
    @Setter
    public static class Services {
        private String databaseUrl;
        private String aiUrl;
    }
}