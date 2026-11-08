package com.example.shoppingwebsite.service;

import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    @Autowired
    private ItemRepository itemRepository;

    @Transactional
    public boolean decreaseStock(Long itemId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        if (!itemRepository.existsById(itemId)) {
            throw new RuntimeException("Item not found");
        }
        return itemRepository.decreaseStockIfAvailable(itemId, quantity) > 0;
    }

    @Transactional
    public void increaseStock(Long itemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (itemRepository.increaseStockBy(itemId, quantity) == 0) {
            throw new RuntimeException("Item not found");
        }
    }

    public int getStockQuantity(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        return item.getStockQuantity();
    }
}