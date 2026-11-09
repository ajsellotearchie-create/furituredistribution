package com.scc.model;

public record OrderLine(int orderItemId, int orderId, int itemId, String itemName,
                        int quantity, double subtotal) {
}