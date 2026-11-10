package com.example.shoppingwebsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShoppingwebsiteApplication {

	public static void main(String[] args) {
		// The H2 web console gives full read/write access to the database. It stays off
		// unless H2_CONSOLE_ENABLED=true is set (local development only).
		if (!"true".equalsIgnoreCase(System.getenv("H2_CONSOLE_ENABLED"))) {
			System.setProperty("spring.h2.console.enabled", "false");
			org.slf4j.LoggerFactory.getLogger(ShoppingwebsiteApplication.class)
					.warn("H2 web console is disabled. Set H2_CONSOLE_ENABLED=true only for local development to turn it on.");
		}
		SpringApplication.run(ShoppingwebsiteApplication.class, args);
	}

}
