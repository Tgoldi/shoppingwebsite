package com.example.shoppingwebsite.repository;

import com.example.shoppingwebsite.model.Order;
import com.example.shoppingwebsite.model.OrderStatus;
import com.example.shoppingwebsite.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    Optional<Order> findByIdAndUser(Long id, User user);

    /** Loads the order with a row lock held until the transaction ends, so two requests cannot change it at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findLockedByIdAndUser(Long id, User user);
    Optional<Order> findByUserAndStatus(User user, OrderStatus status);
    List<Order> findByUserAndStatusNotIn(User user, List<OrderStatus> statuses);


}