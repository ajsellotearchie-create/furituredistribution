package com.scc.model;

public record DistributionOrder(int orderId, int userId, String username, String orderDate,
                                String deliveryAddress, String paymentMethod, double totalAmount,
                                String orderStatus) {
}