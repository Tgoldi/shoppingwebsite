package com.example.shoppingwebsite.model;

public class ItemAvailability {
    private boolean inStock;
    private int availableQuantity;

    public ItemAvailability(boolean inStock, int availableQuantity) {
        this.inStock = inStock;
        this.availableQuantity = availableQuantity;
    }

    public boolean isInStock() {
        return inStock;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    // Setters
    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}