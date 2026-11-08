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
import java.util.function.Function;

import com.example.shoppingwebsite.model.TokenRevocation;
import com.example.shoppingwebsite.repository.TokenRevocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final long ACCESS_TOKEN_LIFETIME_MS = 1000L * 60 * 60 * 10; // 10 hours
    private static final long REFRESH_TOKEN_LIFETIME_MS = 1000L * 60 * 60 * 24; // 24 hours

    private static final String GENERATION_CLAIM = "gen";

    /**
     * Logout state per user, kept in the database (survives restarts, shared by every instance): each token carries
     * the user's generation at issue time, and logout moves the user to the next generation, revoking older tokens.
     */
    @Autowired
    private TokenRevocationRepository tokenRevocationRepository;

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    void warnIfSecretNotFromEnvironment() {
        String fromEnv = System.getenv("JWT_SECRET");
        if (fromEnv == null || fromEnv.isBlank()) {
            logger.warn("JWT_SECRET environment variable is not set: login tokens are signed with the jwt.secret value "
                    + "committed in application.properties, which anyone with the code can read. Set JWT_SECRET "
                    + "(at least 64 random characters) and delete jwt.secret from application.properties.");
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
            logger.warn("Could not extract claims from token: {}", e.getClass().getSimpleName());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(GENERATION_CLAIM, currentGeneration(userDetails.getUsername()));
        return createToken(claims, userDetails.getUsername(), ACCESS_TOKEN_LIFETIME_MS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(GENERATION_CLAIM, currentGeneration(userDetails.getUsername()));
        return createToken(claims, userDetails.getUsername(), REFRESH_TOKEN_LIFETIME_MS);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /** Kept for compatibility: same as validateAccessToken (refresh tokens and revoked tokens are refused). */
    public Boolean validateToken(String token, UserDetails userDetails) {
        return validateAccessToken(token, userDetails);
    }

    /** Valid for API calls: signed, not expired, same user, not a refresh token, not revoked by logout. */
    public Boolean validateAccessToken(String token, UserDetails userDetails) {
        return validateTyped(token, userDetails, false);
    }

    /** Valid for /api/auth/refresh: signed, not expired, same user, a refresh token, not revoked by logout. */
    public Boolean validateRefreshToken(String token, UserDetails userDetails) {
        return validateTyped(token, userDetails, true);
    }

    private boolean validateTyped(String token, UserDetails userDetails, boolean refresh) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            if (username == null || !username.equals(userDetails.getUsername())) {
                return false;
            }
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                return false;
            }
            Object type = claims.get(TOKEN_TYPE_CLAIM);
            if (refresh) {
                if (!REFRESH_TOKEN_TYPE.equals(type)) {
                    return false;
                }
            } else if (REFRESH_TOKEN_TYPE.equals(type)) {
                return false;
            }
            return !isRevoked(username, claims);
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Revokes every token of the user that carries this (verified) token's subject, issued before now.
     * Returns false when the token is not a valid signed token.
     */
    public boolean revokeAllTokensOf(String token) {
        try {
            String username;
            try {
                username = extractUsername(token);
            } catch (io.jsonwebtoken.ExpiredJwtException expired) {
                // Signature was verified before the expiry check: an expired access token still identifies
                // the user, so logout can revoke that user's refresh token too.
                username = expired.getClaims() == null ? null : expired.getClaims().getSubject();
            }
            if (username == null) {
                return false;
            }
            if (tokenRevocationRepository.incrementGeneration(username) == 0) {
                try {
                    tokenRevocationRepository.saveAndFlush(new TokenRevocation(username, 1L));
                } catch (DataIntegrityViolationException alreadyCreated) {
                    // A concurrent logout created the row first: the user already left generation 0.
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("Logout with an invalid token: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    private long currentGeneration(String username) {
        return tokenRevocationRepository.findById(username).map(TokenRevocation::getGeneration).orElse(0L);
    }

    /** A token is revoked when it was issued under an older generation than the user's current one. */
    private boolean isRevoked(String username, Claims claims) {
        Object generation = claims.get(GENERATION_CLAIM);
        // Tokens issued before generations existed carry none: they belong to generation 0.
        long tokenGeneration = generation instanceof Number ? ((Number) generation).longValue() : 0L;
        return tokenGeneration != currentGeneration(username);
    }
}