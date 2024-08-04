package com.example.shoppingwebsite.dto;

public class CartItemRequest {
    private Long itemId;
    private Integer quantity;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItemRequest{" +
                "itemId=" + itemId +
                ", quantity=" + quantity +
                '}';
    }
}
