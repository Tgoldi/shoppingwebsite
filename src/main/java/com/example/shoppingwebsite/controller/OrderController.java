package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.dto.OrderDTO;
import com.example.shoppingwebsite.dto.OrderCreationResponse;
import com.example.shoppingwebsite.dto.UpdateOrderItemRequest;
import com.example.shoppingwebsite.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getUserOrders(Authentication authentication) {
        String userEmail = authentication.getName();
        List<OrderDTO> orders = orderService.getUserOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long orderId, Authentication authentication) {
        String userEmail = authentication.getName();
        return orderService.getOrder(orderId, userEmail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public ResponseEntity<OrderDTO> getUserPendingOrder(Authentication authentication) {
        String userEmail = authentication.getName();
        return orderService.getUserPendingOrder(userEmail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    public ResponseEntity<OrderDTO> addItemToOrder(
            Authentication authentication,
            @RequestParam Long itemId,
            @RequestParam int quantity) {
        String userEmail = authentication.getName();
        try {
            OrderDTO updatedOrder = orderService.addItemToOrder(userEmail, itemId, quantity);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Error adding item to order", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<OrderDTO> removeItemFromOrder(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        logger.info("Received request to remove item from order: orderId={}, orderItemId={}, user={}",
                orderId, orderItemId, userEmail);
        try {
            OrderDTO updatedOrder = orderService.removeItemFromOrder(orderId, orderItemId, userEmail);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Error removing item from order", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<OrderDTO> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestBody UpdateOrderItemRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        logger.info("Received request to update item quantity: orderId={}, orderItemId={}, quantity={}, user={}",
                orderId, orderItemId, request.getQuantity(), userEmail);
        try {
            OrderDTO updatedOrder = orderService.updateItemQuantity(orderId, orderItemId, request.getQuantity(), userEmail);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Error updating item quantity", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{orderId}/close")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            OrderDTO closedOrder = orderService.closeOrder(orderId, userEmail);
            return ResponseEntity.ok(closedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/create-from-cart")
    public ResponseEntity<?> createOrderFromCart(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Creating order from cart for user: {}", userEmail);
            OrderDTO order = orderService.createOrderFromCart(userEmail);
            logger.info("Order created successfully: {}", order.getId());
            return ResponseEntity.ok().body(new OrderCreationResponse(order.getId()));
        } catch (Exception e) {
            logger.error("Error creating order from cart", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getUserOrderHistory(Authentication authentication) {
        String userEmail = authentication.getName();
        try {
            List<OrderDTO> orderHistory = orderService.getUserOrderHistory(userEmail);
            return ResponseEntity.ok(orderHistory);
        } catch (Exception e) {
            logger.error("Error fetching user order history", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderDTO> addItemToExistingOrder(
            @PathVariable Long orderId,
            @RequestParam Long itemId,
            @RequestParam int quantity,
            Authentication authentication) {
        String userEmail = authentication.getName();
        logger.info("Received request to add item to existing order: orderId={}, itemId={}, quantity={}, user={}",
                orderId, itemId, quantity, userEmail);
        try {
            OrderDTO updatedOrder = orderService.addItemToExistingOrder(orderId, itemId, quantity, userEmail);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Error adding item to existing order", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

}