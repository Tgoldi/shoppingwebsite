package com.example.shoppingwebsite.config;

import com.example.shoppingwebsite.security.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${ADMIN_API_TOKEN:}")
    private String adminApiToken;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // The H2 database console must never be reachable over HTTP.
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).denyAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/refresh-token").permitAll()
                        // Catalogue reads stay public; every other method on /api/items (stock changes)
                        // is an operator action and needs the X-Admin-Token header (denied when unset).
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/**").permitAll()
                        .requestMatchers("/api/items", "/api/items/**").access(adminTokenAuthorizationManager())
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/profile").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) -> {
                            logger.error("Unauthorized error: {}", authException.getMessage());
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"" + authException.getMessage() + "\"}");
                        })
                );

        return http.build();
    }

    /**
     * Grants stock-changing item requests only when the X-Admin-Token header matches the
     * ADMIN_API_TOKEN environment variable. When ADMIN_API_TOKEN is not set, they are always denied.
     */
    private AuthorizationManager<RequestAuthorizationContext> adminTokenAuthorizationManager() {
        final byte[] expected = adminApiToken == null ? new byte[0] : adminApiToken.trim().getBytes(StandardCharsets.UTF_8);
        return (authentication, context) -> {
            if (expected.length == 0) {
                return new AuthorizationDecision(false);
            }
            String provided = context.getRequest().getHeader("X-Admin-Token");
            if (provided == null || provided.isEmpty()) {
                return new AuthorizationDecision(false);
            }
            boolean ok = MessageDigest.isEqual(expected, provided.trim().getBytes(StandardCharsets.UTF_8));
            return new AuthorizationDecision(ok);
        };
    }

    @PostConstruct
    void warnIfAdminTokenMissing() {
        if (adminApiToken == null || adminApiToken.isBlank()) {
            logger.warn("ADMIN_API_TOKEN is not set: stock-changing endpoints under /api/items are disabled until it is set "
                    + "(callers must then send it in the X-Admin-Token header).");
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}