package com.example.shoppingwebsite.service;

import com.example.shoppingwebsite.model.*;
import com.example.shoppingwebsite.repository.OrderRepository;
import com.example.shoppingwebsite.repository.UserRepository;
import com.example.shoppingwebsite.repository.ItemRepository;
import com.example.shoppingwebsite.dto.OrderDTO;
import com.example.shoppingwebsite.dto.OrderItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private StockService stockService;

    public List<OrderDTO> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Order> orders = orderRepository.findByUser(user);
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Optional<OrderDTO> getOrder(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByIdAndUser(orderId, user).map(this::convertToDTO);
    }

    public Optional<OrderDTO> getUserPendingOrder(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserAndStatus(user, OrderStatus.TEMP).map(this::convertToDTO);
    }

    @Transactional
    public OrderDTO addItemToOrder(String userEmail, Long itemId, int quantity) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Order order = getOrCreatePendingOrder(user);

        Optional<OrderItem> existingItem = order.getOrderItems().stream()
                .filter(oi -> oi.getItem().getId().equals(itemId))
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem orderItem = existingItem.get();
            int newQuantity = orderItem.getQuantity() + quantity;
            if (newQuantity > 2) {
                throw new RuntimeException("Cannot add more than 2 of the same item");
            }
            orderItem.setQuantity(newQuantity);
        } else {
            if (quantity > 2) {
                throw new RuntimeException("Cannot add more than 2 of the same item");
            }
            OrderItem newItem = new OrderItem();
            newItem.setOrder(order);
            newItem.setItem(item);
            newItem.setQuantity(quantity);
            newItem.setPrice(item.getPrice());
            order.getOrderItems().add(newItem);
        }

        updateOrderTotalPrice(order);
        return convertToDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO removeItemFromOrder(Long orderId, Long orderItemId, String userEmail) {
        logger.info("Removing item from order: orderId={}, orderItemId={}, userEmail={}",
                orderId, orderItemId, userEmail);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getUser().getEmail().equals(userEmail)) {
                throw new RuntimeException("User not authorized to modify this order");
            }

            boolean removed = order.getOrderItems().removeIf(item -> item.getId().equals(orderItemId));
            if (!removed) {
                throw new RuntimeException("Item not found in order");
            }

            updateOrderTotalPrice(order);

            // If the order is empty after removing the item, set its status to CANCELED instead of deleting it
            if (order.getOrderItems().isEmpty()) {
                order.setStatus(OrderStatus.CANCELED);
            }

            Order updatedOrder = orderRepository.save(order);
            logger.info("Item removed successfully: orderId={}", orderId);
            return convertToDTO(updatedOrder);
        } catch (Exception e) {
            logger.error("Error removing item from order", e);
            throw new RuntimeException("Failed to remove item from order: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDTO updateItemQuantity(Long orderId, Long orderItemId, int newQuantity, String userEmail) {
        logger.info("Updating item quantity: orderId={}, orderItemId={}, newQuantity={}, userEmail={}",
                orderId, orderItemId, newQuantity, userEmail);
        try {
            Order order = getOrder(orderId, userEmail)
                    .map(this::convertToEntity)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            OrderItem orderItem = order.getOrderItems().stream()
                    .filter(item -> item.getId().equals(orderItemId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Item not found in order"));

            if (newQuantity > 2) {
                throw new RuntimeException("Cannot add more than 2 of the same item");
            } else if (newQuantity <= 0) {
                order.getOrderItems().remove(orderItem);
            } else {
                orderItem.setQuantity(newQuantity);
            }

            updateOrderTotalPrice(order);
            OrderDTO updatedOrder = convertToDTO(orderRepository.save(order));
            logger.info("Order updated successfully: orderId={}", orderId);
            return updatedOrder;
        } catch (Exception e) {
            logger.error("Error updating item quantity", e);
            throw new RuntimeException("Failed to update item quantity: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDTO closeOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("User not authorized to close this order");
        }

        if (order.getStatus() != OrderStatus.TEMP) {
            throw new RuntimeException("Order is already closed");
        }

        // Update item quantities
        for (OrderItem orderItem : order.getOrderItems()) {
            Item item = orderItem.getItem();
            int newStockQuantity = item.getStockQuantity() - orderItem.getQuantity();
            if (newStockQuantity < 0) {
                throw new RuntimeException("Not enough stock for item: " + item.getName());
            }
            item.setStockQuantity(newStockQuantity);
            itemRepository.save(item);
        }

        order.setStatus(OrderStatus.CLOSED);
        Order savedOrder = orderRepository.save(order);

        // Clear the user's cart
        User user = order.getUser();
        Cart cart = user.getCart();
        if (cart != null) {
            cart.getItems().clear();
            user.setCart(cart);
            userRepository.save(user);
        }

        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO createOrderFromCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.TEMP);
        order.setShippingAddress(user.getCountry() + ", " + user.getCity());
        order.setOrderDate(LocalDateTime.now());

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getItem().getPrice());
            order.getOrderItems().add(orderItem);
        }

        updateOrderTotalPrice(order);
        Order savedOrder = orderRepository.save(order);

        return convertToDTO(savedOrder);
    }

    private Order getOrCreatePendingOrder(User user) {
        return orderRepository.findByUserAndStatus(user, OrderStatus.TEMP)
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setUser(user);
                    newOrder.setStatus(OrderStatus.TEMP);
                    newOrder.setTotalPrice(BigDecimal.ZERO);
                    return orderRepository.save(newOrder);
                });
    }

    private void updateOrderTotalPrice(Order order) {
        BigDecimal total = order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(total);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().toString());

        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(orderItemDTOs);

        return dto;
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setItemId(orderItem.getItem().getId());
        dto.setItemName(orderItem.getItem().getName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setImageUrl(orderItem.getItem().getImageUrl());
        return dto;
    }

    private Order convertToEntity(OrderDTO orderDTO) {
        Order order = new Order();
        order.setId(orderDTO.getId());
        order.setUser(userRepository.findById(orderDTO.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        order.setOrderDate(orderDTO.getOrderDate());
        order.setShippingAddress(orderDTO.getShippingAddress());
        order.setTotalPrice(orderDTO.getTotalPrice());
        order.setStatus(OrderStatus.valueOf(orderDTO.getStatus()));

        List<OrderItem> orderItems = orderDTO.getOrderItems().stream()
                .map(this::convertToOrderItemEntity)
                .collect(Collectors.toList());
        order.setOrderItems(orderItems);

        return order;
    }

    private OrderItem convertToOrderItemEntity(OrderItemDTO dto) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(dto.getId());
        orderItem.setItem(itemRepository.findById(dto.getItemId()).orElseThrow(() -> new RuntimeException("Item not found")));
        orderItem.setQuantity(dto.getQuantity());
        orderItem.setPrice(dto.getPrice());
        return orderItem;
    }

    public List<OrderDTO> getUserOrderHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Order> orders = orderRepository.findByUserAndStatusNotIn(user, Arrays.asList(OrderStatus.TEMP, OrderStatus.CANCELED));
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    @Transactional
    public OrderDTO addItemToExistingOrder(Long orderId, Long itemId, int quantity, String userEmail) {
        logger.info("Adding item to existing order: orderId={}, itemId={}, quantity={}, userEmail={}",
                orderId, itemId, quantity, userEmail);
        try {
            Order order = getOrder(orderId, userEmail)
                    .map(this::convertToEntity)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            Optional<OrderItem> existingItem = order.getOrderItems().stream()
                    .filter(oi -> oi.getItem().getId().equals(itemId))
                    .findFirst();

            if (existingItem.isPresent()) {
                OrderItem orderItem = existingItem.get();
                int newQuantity = orderItem.getQuantity() + quantity;
                if (newQuantity > 2) {
                    throw new RuntimeException("Cannot add more than 2 of the same item");
                }
                orderItem.setQuantity(newQuantity);
            } else {
                if (quantity > 2) {
                    throw new RuntimeException("Cannot add more than 2 of the same item");
                }
                OrderItem newItem = new OrderItem();
                newItem.setOrder(order);
                newItem.setItem(item);
                newItem.setQuantity(quantity);
                newItem.setPrice(item.getPrice());
                order.getOrderItems().add(newItem);
            }

            updateOrderTotalPrice(order);

            // If the order was previously CANCELED and now has items, set it back to TEMP
            if (order.getStatus() == OrderStatus.CANCELED && !order.getOrderItems().isEmpty()) {
                order.setStatus(OrderStatus.TEMP);
            }

            return convertToDTO(orderRepository.save(order));
        } catch (Exception e) {
            logger.error("Error adding item to existing order", e);
            throw new RuntimeException("Failed to add item to existing order: " + e.getMessage());
        }
    }
}