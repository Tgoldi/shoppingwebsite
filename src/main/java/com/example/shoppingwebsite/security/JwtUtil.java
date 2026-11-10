package com.example.shoppingwebsite.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ISSUED_AT_MS_CLAIM = "iat_ms";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final long ACCESS_TOKEN_MS = 1000L * 60 * 60 * 10; // 10 hours
    private static final long REFRESH_TOKEN_MS = 1000L * 60 * 60 * 24; // 24 hours

    // username -> tokens issued at or before this time (ms) are revoked (set on logout)
    private final Map<String, Long> revokedBefore = new ConcurrentHashMap<>();

    @Value("${jwt.secret}")
    private String secret;

    @jakarta.annotation.PostConstruct
    void warnIfSecretNotFromEnvironment() {
        String fromEnv = System.getenv("JWT_SECRET");
        if (fromEnv == null || fromEnv.isBlank()) {
            logger.warn("JWT_SECRET environment variable is not set: login tokens are signed with the jwt.secret value "
                    + "committed in application.properties, so anyone with the code can forge them. "
                    + "Set JWT_SECRET to a long random value (at least 32 characters) and remove jwt.secret from application.properties.");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            // Never log the token itself: it is a bearer credential.
            logger.error("Could not extract claims from token: {}", e.getClass().getSimpleName());
            throw e;
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TYPE_ACCESS);
        return createToken(claims, userDetails.getUsername(), ACCESS_TOKEN_MS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TYPE_REFRESH);
        return createToken(claims, userDetails.getUsername(), REFRESH_TOKEN_MS);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        long now = System.currentTimeMillis();
        // "iat" only has whole-second precision; keep the exact issue time for logout revocation
        claims.put(ISSUED_AT_MS_CLAIM, now);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /** Validates an access token (the only kind accepted for authenticating API requests). */
    public Boolean validateToken(String token, UserDetails userDetails) {
        return validateTokenOfType(token, userDetails, TYPE_ACCESS);
    }

    /** Validates a refresh token (the only kind accepted by the refresh endpoint). */
    public Boolean validateRefreshToken(String token, UserDetails userDetails) {
        return validateTokenOfType(token, userDetails, TYPE_REFRESH);
    }

    private boolean validateTokenOfType(String token, UserDetails userDetails, String expectedType) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            return username != null
                    && username.equals(userDetails.getUsername())
                    && expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && claims.getExpiration() != null
                    && !claims.getExpiration().before(new Date())
                    && !isRevoked(username, claims.get(ISSUED_AT_MS_CLAIM, Long.class));
        } catch (Exception e) {
            // Never log the token itself: it is a bearer credential.
            logger.error("Token validation failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean isRevoked(String username, Long issuedAtMs) {
        Long cutoff = revokedBefore.get(username);
        if (cutoff == null) {
            return false;
        }
        return issuedAtMs == null || issuedAtMs <= cutoff;
    }

    /**
     * Revokes every token (access and refresh) issued to the subject of the given, validly signed
     * and unexpired token up to now. Tokens issued afterwards (a new login) stay valid.
     * Throws if the token is not valid, so an unauthenticated caller cannot log anyone out.
     */
    public void revokeAllTokensOf(String token) {
        final Claims claims = extractAllClaims(token);
        final String username = claims.getSubject();
        if (username == null) {
            return;
        }
        long now = System.currentTimeMillis();
        revokedBefore.put(username, now);
        // Forget cut-offs older than the longest token lifetime: every token they cover has expired
        revokedBefore.values().removeIf(cutoff -> cutoff < now - REFRESH_TOKEN_MS);
    }
}