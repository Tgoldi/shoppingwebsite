package com.example.shoppingwebsite.service;

import com.example.shoppingwebsite.model.FavoriteItem;
import com.example.shoppingwebsite.model.Item;
import com.example.shoppingwebsite.model.User;
import com.example.shoppingwebsite.repository.FavoriteItemRepository;
import com.example.shoppingwebsite.repository.UserRepository;
import com.example.shoppingwebsite.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private FavoriteItemRepository favoriteItemRepository;

    @Autowired
    private ItemService itemService;

    public List<Item> getFavoriteItems(String userEmail) {
        Optional<User> user = userRepository.findByEmail(userEmail);
        return favoriteItemRepository.findByUser(user).stream()
                .map(FavoriteItem::getItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToFavorites(String userEmail, Long itemId) {
        Optional<User> userOptional = userRepository.findByEmail(userEmail);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!favoriteItemRepository.existsByUserAndItemId(userOptional, itemId)) {
                Item item = itemService.getItemById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found"));
                FavoriteItem favoriteItem = new FavoriteItem();
                favoriteItem.setUser(user);
                favoriteItem.setItem(item);
                favoriteItemRepository.save(favoriteItem);
            }
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    @Transactional
    public void removeFromFavorites(String userEmail, Long itemId) {
        Optional<User> user = userRepository.findByEmail(userEmail);
        favoriteItemRepository.deleteByUserAndItemId(user, itemId);
    }
}
