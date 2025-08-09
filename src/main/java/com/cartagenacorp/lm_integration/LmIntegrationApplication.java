package com.cartagenacorp.lm_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class LmIntegrationApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(LmIntegrationApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(LmIntegrationApplication.class);
	}

}
