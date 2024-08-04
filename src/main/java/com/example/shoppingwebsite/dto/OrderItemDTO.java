package com.example.shoppingwebsite.dto;

import java.math.BigDecimal;

public class OrderItemDTO {
    private Long id;
    private Long itemId;
    private String itemName;
    private int quantity;
    private BigDecimal price;

    private String imageUrl;

    public OrderItemDTO(Long id, Long itemId, String itemName, int quantity, BigDecimal price, String imageUrl) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public OrderItemDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}