package com.dressme.dressme_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.dressme.dressme_back.client")
public class DressmeBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(DressmeBackApplication.class, args);
	}

}
