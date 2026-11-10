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
        if (!itemRepository.existsById(itemId)) {
            throw new RuntimeException("Item not found");
        }
        if (quantity <= 0) {
            return false;
        }
        // One conditional UPDATE, so concurrent decreases cannot push stock below zero.
        return itemRepository.decrementStockIfAvailable(itemId, quantity) == 1;
    }

    @Transactional
    public void increaseStock(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        item.setStockQuantity(item.getStockQuantity() + quantity);
        itemRepository.save(item);
    }

    public int getStockQuantity(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        return item.getStockQuantity();
    }
}