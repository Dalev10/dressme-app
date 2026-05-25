package com.dressme.dressme_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.dressme.dressme_gateway.config.AppProperties;
import com.dressme.dressme_gateway.config.GoogleProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, GoogleProperties.class})
public class DressmeGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(DressmeGatewayApplication.class, args);
	}

}
