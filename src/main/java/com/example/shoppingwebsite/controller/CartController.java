package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.dto.CartItemRequest;
import com.example.shoppingwebsite.exception.ItemNotFoundException;
import com.example.shoppingwebsite.model.Cart;
import com.example.shoppingwebsite.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            String userEmail = authentication.getName();
            logger.info("Retrieving cart for user: {}", userEmail);
            Cart cart = cartService.getCartForUser(userEmail);
            if (cart == null || cart.getItems().isEmpty()) {
                logger.info("Cart is empty for user: {}", userEmail);
                return ResponseEntity.ok(Collections.emptyList());
            }
            logger.info("Cart retrieved successfully for user: {}", userEmail);
            return ResponseEntity.ok(cart.getItems());
        } catch (Exception e) {
            logger.error("Error retrieving cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving cart: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody CartItemRequest request, Authentication authentication) {
        if (authentication == null) {
            logger.error("User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        logger.info("Received add to cart request: {}", request);

        if (request == null) {
            logger.error("Request body is null");
            return ResponseEntity.badRequest().body("Request body is null");
        }
        if (request.getItemId() == null) {
            logger.error("Item ID is required");
            return ResponseEntity.badRequest().body("Item ID is required");
        }
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            logger.error("Invalid quantity: {}", quantity);
            return ResponseEntity.badRequest().body("Invalid quantity");
        }

        try {
            String userEmail = authentication.getName();
            logger.info("Adding item to cart for user: {}, itemId: {}, quantity: {}", userEmail, request.getItemId(), quantity);
            Cart updatedCart = cartService.addToCart(userEmail, request.getItemId(), quantity);
            logger.info("Item added successfully to cart for user: {}", userEmail);
            return ResponseEntity.ok(updatedCart);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (ItemNotFoundException e) {
            logger.error("Item not found: {}", request.getItemId(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
        } catch (Exception e) {
            logger.error("Error adding to cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding to cart: " + e.getMessage());
        }
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long itemId, @RequestBody CartItemRequest request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        Integer quantity = request.getQuantity();
        if (request == null || quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body("Invalid request");
        }
        try {
            String userEmail = authentication.getName();
            logger.info("Updating cart item for user: {}, itemId: {}, quantity: {}", userEmail, itemId, quantity);
            cartService.updateCartItem(userEmail, itemId, quantity);
            Cart updatedCart = cartService.getCartForUser(userEmail);
            logger.info("Cart item updated successfully for user: {}", userEmail);
            return ResponseEntity.ok(updatedCart);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (ItemNotFoundException e) {
            logger.error("Item not found in cart: {}", itemId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found in cart");
        } catch (Exception e) {
            logger.error("Error updating cart item for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating cart item: " + e.getMessage());
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long itemId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            String userEmail = authentication.getName();
            logger.info("Removing item from cart for user: {}, itemId: {}", userEmail, itemId);
            cartService.removeCartItem(userEmail, itemId);
            Cart updatedCart = cartService.getCartForUser(userEmail);
            logger.info("Item removed successfully from cart for user: {}", userEmail);
            return ResponseEntity.ok(updatedCart);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            logger.error("Error removing cart item for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing cart item: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            String userEmail = authentication.getName();
            logger.info("Clearing cart for user: {}", userEmail);
            cartService.clearCart(userEmail);
            Cart emptyCart = cartService.getCartForUser(userEmail);
            logger.info("Cart cleared successfully for user: {}", userEmail);
            return ResponseEntity.ok(emptyCart);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            logger.error("Error clearing cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing cart: " + e.getMessage());
        }
    }

    @GetMapping("/total")
    public ResponseEntity<?> getCartTotal(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            String userEmail = authentication.getName();
            logger.info("Retrieving cart total for user: {}", userEmail);
            BigDecimal total = cartService.getCartTotal(userEmail);
            logger.info("Cart total retrieved successfully for user: {}", userEmail);
            return ResponseEntity.ok(total);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            logger.error("Error retrieving cart total for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving cart total: " + e.getMessage());
        }
    }
}
