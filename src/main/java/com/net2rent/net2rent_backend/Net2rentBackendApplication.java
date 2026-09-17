package com.net2rent.net2rent_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@ConfigurationPropertiesScan
public class Net2rentBackendApplication {
	private static final Logger log = LoggerFactory.getLogger(Net2rentBackendApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(Net2rentBackendApplication.class, args);
		log.info(">>> Net2rent backend is up and running");
	}
}