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
     * Atomically takes quantity units from stock, only when enough units are left.
     * Returns 1 when stock was decreased, 0 when there was not enough stock (or no such item).
     */
    @Modifying
    @Query("UPDATE Item i SET i.stockQuantity = i.stockQuantity - :quantity WHERE i.id = :id AND i.stockQuantity >= :quantity")
    int decreaseStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    /** Atomically adds quantity units to stock. Returns the number of rows changed. */
    @Modifying
    @Query("UPDATE Item i SET i.stockQuantity = i.stockQuantity + :quantity WHERE i.id = :id")
    int increaseStockBy(@Param("id") Long id, @Param("quantity") int quantity);
}
