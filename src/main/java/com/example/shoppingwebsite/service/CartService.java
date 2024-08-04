package com.example.shoppingwebsite.service;

import com.example.shoppingwebsite.exception.ItemNotFoundException;
import com.example.shoppingwebsite.model.Cart;
import com.example.shoppingwebsite.model.CartItem;
import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.model.User;
import com.example.shoppingwebsite.repository.ItemRepository;
import com.example.shoppingwebsite.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private final UserService userService;

    @Autowired
    public CartService(UserService userService) {
        this.userService = userService;
    }

    public Cart getCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        logger.info("Retrieved cart for user: {}", userEmail);
        return user.getCart();
    }

    @Transactional
    public Cart addToCart(String userEmail, Long itemId, int quantity) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));

        Cart cart = user.getCart();
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            user.setCart(cart);
        }

        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (cartItem == null) {
            cartItem = new CartItem(cart, item, quantity);
            cart.addItem(cartItem);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        userRepository.save(user);
        logger.info("Added item {} to cart for user {}", itemId, userEmail);
        return cart;
    }

    @Transactional
    public void updateCartItem(String userEmail, Long itemId, int quantity) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        Cart cart = user.getCart();

        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Item not found in cart: " + itemId));

        cartItem.setQuantity(quantity);
        userRepository.save(user);
        logger.info("Updated item {} quantity to {} for user {}", itemId, quantity, userEmail);
    }

    @Transactional
    public void removeCartItem(String userEmail, Long itemId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        Cart cart = user.getCart();

        cart.getItems().removeIf(ci -> ci.getItem().getId().equals(itemId));
        userRepository.save(user);
        logger.info("Removed item {} from cart for user {}", itemId, userEmail);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        Cart cart = user.getCart();

        if (cart != null) {
            cart.getItems().clear();
            userRepository.save(user);
            logger.info("Cleared cart for user {}", userEmail);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

            Cart cart = user.getCart();
            if (cart == null) {
                logger.info("Cart is null for user: {}", userEmail);
                return BigDecimal.ZERO;
            }

            BigDecimal total = cart.getItems().stream()
                    .filter(ci -> ci.getItem() != null && ci.getItem().getPrice() != null)
                    .map(ci -> ci.getItem().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            logger.info("Calculated cart total for user {}: {}", userEmail, total);
            return total;
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", userEmail);
            throw e;
        } catch (Exception e) {
            logger.error("Error calculating cart total for user {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Error calculating cart total", e);
        }
    }

    public Cart getCartForUser(String email) {
        User user = userService.getUserProfile(email);
        Cart cart = user.getCart();
        if (cart != null) {
            cart.getItems().size();
        }
        logger.info("Retrieved cart for user profile: {}", email);
        return cart;
    }
}
