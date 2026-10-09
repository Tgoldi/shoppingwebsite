package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.dto.LoginRequest;
import com.example.shoppingwebsite.model.User;
import com.example.shoppingwebsite.security.JwtUtil;
import com.example.shoppingwebsite.security.RefreshTokenRequest;
import com.example.shoppingwebsite.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // In-memory rate limiter: resets on restart and counts per server instance.
    private static final long RATE_WINDOW_MS = 60_000L;
    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final int REGISTER_MAX_ATTEMPTS = 5;
    private static final int RATE_MAP_MAX_SIZE = 10_000;
    private static final Map<String, long[]> RATE_LIMIT_ENTRIES = new ConcurrentHashMap<>();

    static {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auth-rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, long[]>> it = RATE_LIMIT_ENTRIES.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue()[1] > RATE_WINDOW_MS) {
                    it.remove();
                }
            }
        }, RATE_WINDOW_MS, RATE_WINDOW_MS, TimeUnit.MILLISECONDS);
    }

    private static boolean isRateLimited(String key, int max) {
        long now = System.currentTimeMillis();
        if (RATE_LIMIT_ENTRIES.size() >= RATE_MAP_MAX_SIZE && !RATE_LIMIT_ENTRIES.containsKey(key)) {
            return true;
        }
        long[] entry = RATE_LIMIT_ENTRIES.compute(key, (k, v) -> {
            if (v == null || now - v[1] > RATE_WINDOW_MS) {
                return new long[]{1, now};
            }
            v[0]++;
            return v;
        });
        return entry[0] > max;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        if (isRateLimited("login:" + httpRequest.getRemoteAddr() + ":" + loginRequest.getEmail(), LOGIN_MAX_ATTEMPTS)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many login attempts. Try again later.");
        }
        logger.info("Login attempt for user: {}", loginRequest.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("refreshToken", refreshToken);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for user: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } catch (Exception e) {
            logger.error("Error during login process for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().body("Logged out successfully");
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody User requestUser, HttpServletRequest httpRequest) {
        if (isRateLimited("register:" + httpRequest.getRemoteAddr(), REGISTER_MAX_ATTEMPTS)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many registration attempts. Try again later.");
        }
        try {
            // Only copy the fields a client may set; ignore id/cart/favorites etc.
            User user = new User();
            user.setEmail(requestUser.getEmail());
            user.setPassword(requestUser.getPassword());
            user.setName(requestUser.getName());

            // Check if user already exists
            UserDetails existingUser = null;
            try {
                existingUser = userService.loadUserByUsername(user.getEmail());
            } catch (UsernameNotFoundException e) {
                // User not found
            }

            if (existingUser != null) {
                return ResponseEntity.badRequest().body("Email already in use");
            }

            User registeredUser = userService.registerUser(user);
            UserDetails userDetails = userService.loadUserByUsername(registeredUser.getEmail());

            String token = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("refreshToken", refreshToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during user registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during registration");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String token = refreshTokenRequest.getToken();
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                String newToken = jwtUtil.generateToken(userDetails);
                Map<String, String> response = new HashMap<>();
                response.put("token", newToken);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (ExpiredJwtException e) {
            logger.error("Token has expired", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token has expired");
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error refreshing token");
        }
    }
}