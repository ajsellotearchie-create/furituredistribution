package com.scc.model;

public record FurnitureItem(int itemId, String itemName, String category, String dimensions,
                            String material, double price, int stockQuantity, String warehouseLocation) {
}