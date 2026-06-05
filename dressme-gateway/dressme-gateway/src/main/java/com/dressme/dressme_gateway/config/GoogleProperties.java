package com.dressme.dressme_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private OAuth oauth = new OAuth();

    public static class OAuth {
        private String userinfoUrl;

        public String getUserinfoUrl() {
            return userinfoUrl;
        }

        public void setUserinfoUrl(String userinfoUrl) {
            this.userinfoUrl = userinfoUrl;
        }
    }

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth;
    }
}
