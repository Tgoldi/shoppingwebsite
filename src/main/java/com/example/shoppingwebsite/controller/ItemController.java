package com.example.shoppingwebsite.controller;

import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.model.ItemAvailability;
import com.example.shoppingwebsite.service.ItemService;
import com.example.shoppingwebsite.service.StockService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private StockService stockService;

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    @Value("${STOCK_ADMIN_KEY:}")
    private String stockAdminKey;

    @PostConstruct
    void warnIfStockAdminKeyMissing() {
        if (stockAdminKey == null || stockAdminKey.isBlank()) {
            logger.warn("STOCK_ADMIN_KEY is not set: PUT /api/items/{id}/stock/increase and /stock/decrease are disabled. "
                    + "Set STOCK_ADMIN_KEY and send it in the X-Admin-Key header to use them.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchItems(@RequestParam String query) {
        List<Item> items = itemService.searchItems(query);
        if (items.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Integer> getStockQuantity(@PathVariable Long id) {
        int quantity = stockService.getStockQuantity(id);
        return ResponseEntity.ok(quantity);
    }

    /**
     * Stock changes are an operator action: customers' orders update stock on the server
     * (OrderService.closeOrder). These endpoints require the X-Admin-Key header to match
     * the STOCK_ADMIN_KEY environment variable, and are refused when it is not set.
     */
    private ResponseEntity<?> checkStockAdmin(String providedKey, int quantity) {
        if (stockAdminKey == null || stockAdminKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock changes are disabled");
        }
        if (providedKey == null || !MessageDigest.isEqual(
                stockAdminKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be positive");
        }
        return null;
    }

    @PutMapping("/{id}/stock/decrease")
    public ResponseEntity<?> decreaseStock(@PathVariable Long id, @RequestParam int quantity,
                                           @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey) {
        ResponseEntity<?> denied = checkStockAdmin(adminKey, quantity);
        if (denied != null) {
            return denied;
        }
        boolean decreased = stockService.decreaseStock(id, quantity);
        if (decreased) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Not enough stock");
        }
    }

    @PutMapping("/{id}/stock/increase")
    public ResponseEntity<?> increaseStock(@PathVariable Long id, @RequestParam int quantity,
                                           @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey) {
        ResponseEntity<?> denied = checkStockAdmin(adminKey, quantity);
        if (denied != null) {
            return denied;
        }
        stockService.increaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{itemId}/availability")
    public ResponseEntity<?> checkItemAvailability(@PathVariable Long itemId) {
        logger.info("Checking availability for itemId: {}", itemId);
        try {
            int quantity = stockService.getStockQuantity(itemId);
            boolean inStock = quantity > 0;
            ItemAvailability availability = new ItemAvailability(inStock, quantity);
            logger.info("Availability for itemId {}: inStock={}, quantity={}", itemId, inStock, quantity);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            logger.error("Error checking item availability for itemId {}: {}", itemId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error checking item availability");
        }
    }
}