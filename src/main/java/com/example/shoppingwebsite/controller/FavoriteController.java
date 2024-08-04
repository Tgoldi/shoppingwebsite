package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<Item>> getFavoriteItems(Authentication authentication) {
        String userEmail = authentication.getName();
        List<Item> favoriteItems = favoriteService.getFavoriteItems(userEmail);
        return ResponseEntity.ok(favoriteItems);
    }

    @PostMapping("/{itemId}")
    public ResponseEntity<?> addToFavorites(@PathVariable Long itemId, Authentication authentication) {
        String userEmail = authentication.getName();
        favoriteService.addToFavorites(userEmail, itemId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeFromFavorites(@PathVariable Long itemId, Authentication authentication) {
        String userEmail = authentication.getName();
        favoriteService.removeFromFavorites(userEmail, itemId);
        return ResponseEntity.ok().build();
    }
}
