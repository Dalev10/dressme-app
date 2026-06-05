package com.dressme.dressme_back.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "supabase")
@Getter
@Setter
public class SupabaseStorageProperties {
    private String url;
    private String serviceKey;
    private String bucket;
}
