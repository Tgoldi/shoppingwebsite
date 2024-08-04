package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.model.User;
import com.example.shoppingwebsite.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<User> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserProfile(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateUserProfile(Authentication authentication, @RequestBody User updatedUser) {
        String email = authentication.getName();
        User user = userService.updateUserProfile(email, updatedUser);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteUserProfile(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            userService.deleteUser(userEmail);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting user: " + e.getMessage());
        }
    }
}