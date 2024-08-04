package com.example.shoppingwebsite.repository;

import com.example.shoppingwebsite.model.FavoriteItem;
import com.example.shoppingwebsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    List<FavoriteItem> findByUser(Optional<User> user);
    boolean existsByUserAndItemId(Optional<User> user, Long itemId);
    void deleteByUserAndItemId(Optional<User> user, Long itemId);
}