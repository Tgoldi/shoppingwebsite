package com.example.shoppingwebsite.dto;

public class OrderCreationResponse {
    private Long orderId;

    public OrderCreationResponse(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
