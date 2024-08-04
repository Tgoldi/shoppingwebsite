package com.example.shoppingwebsite.config;

import com.example.shoppingwebsite.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class JwtConfig {
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}
