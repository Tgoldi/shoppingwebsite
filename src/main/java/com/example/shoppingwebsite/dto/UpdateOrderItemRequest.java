package com.example.shoppingwebsite.dto;

public class UpdateOrderItemRequest {
    private int quantity;

    public UpdateOrderItemRequest() {}

    public UpdateOrderItemRequest(int quantity) {
        this.quantity = quantity;
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}