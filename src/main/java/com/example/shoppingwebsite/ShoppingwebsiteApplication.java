package com.example.shoppingwebsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShoppingwebsiteApplication {

	public static void main(String[] args) {
		// The H2 web console gives full access to the database and has no login of its own:
		// keep it switched off (a system property overrides application.properties) unless the
		// SPRING_H2_CONSOLE_ENABLED environment variable is set. Even then, SecurityConfig refuses
		// every request to /h2-console over HTTP.
		if (System.getenv("SPRING_H2_CONSOLE_ENABLED") == null
				&& System.getProperty("spring.h2.console.enabled") == null) {
			System.setProperty("spring.h2.console.enabled", "false");
		}
		SpringApplication.run(ShoppingwebsiteApplication.class, args);
	}

}
