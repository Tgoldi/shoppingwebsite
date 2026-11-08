package com.example.shoppingwebsite.service;

import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public List<Item> searchItems(String query) {
        return itemRepository.findByNameContainingIgnoreCase(query);
    }

    public Item saveItem(Item item) {
        return itemRepository.save(item);
    }

    @Transactional
    public boolean updateStock(Long itemId, int quantity) {
        if (!itemRepository.existsById(itemId)) {
            throw new RuntimeException("Item not found");
        }
        if (quantity <= 0) {
            return false;
        }
        return itemRepository.decreaseStockIfAvailable(itemId, quantity) > 0;
    }
}