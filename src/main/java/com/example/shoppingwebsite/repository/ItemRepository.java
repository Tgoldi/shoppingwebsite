package com.example.shoppingwebsite.repository;

import com.example.shoppingwebsite.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByNameContainingIgnoreCase(String name);

    /**
     * Atomically takes {@code quantity} units out of stock, only if that many are available.
     * Returns the number of rows changed: 1 on success, 0 when the item is missing or stock is short.
     */
    @Modifying
    @Query("UPDATE Item i SET i.stockQuantity = i.stockQuantity - :quantity "
            + "WHERE i.id = :itemId AND i.stockQuantity >= :quantity")
    int decrementStockIfAvailable(@Param("itemId") Long itemId, @Param("quantity") int quantity);
}
