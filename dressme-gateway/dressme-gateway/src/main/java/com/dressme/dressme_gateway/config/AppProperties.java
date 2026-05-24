package com.dressme.dressme_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Services services = new Services();

    public static class Services {
        private String backendUrl;

        public String getBackendUrl() {
            return backendUrl;
        }

        public void setBackendUrl(String backendUrl) {
            this.backendUrl = backendUrl;
        }
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}
