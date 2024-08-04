package com.example.shoppingwebsite.dto;

public class CreateOrderRequest {
    private String shippingAddress;


    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
