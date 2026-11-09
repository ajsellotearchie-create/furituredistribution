package com.scc.dao;

import com.scc.model.DistributionOrder;
import com.scc.model.OrderLine;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDao {

    public static final String PROCESSING = "Processing";
    public static final String SHIPPED = "Shipped";
    public static final String DELIVERED = "Delivered";
    public static final String CANCELLED = "Cancelled";

    // Payment methods offered when creating an order
    public static final List<String> PAYMENT_METHODS =
            List.of("Cash on Delivery", "Bank Transfer", "Credit/Debit Card", "E-Wallet");

    // ---------------- Create ----------------

    // Creates the order and deducts stock in ONE transaction.
    // If any item is missing or short on stock, everything is rolled back.
    // cart: item_id -> quantity. Returns the new order_id, or -1 on failure.
    public int createOrder(int userId, String address, String paymentMethod, Map<Integer, Integer> cart) {
        if (cart.isEmpty()) return -1;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId = insertOrder(conn, userId, address, paymentMethod);
                double total = 0;
                for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                    total += addLine(conn, orderId, e.getKey(), e.getValue());
                }
                try (PreparedStatement p = conn.prepareStatement(
                        "UPDATE DistributionOrders SET total_amount = ? WHERE order_id = ?")) {
                    p.setDouble(1, total);
                    p.setInt(2, orderId);
                    p.executeUpdate();
                }
                conn.commit();
                return orderId;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Order failed: " + e.getMessage());
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Order failed: " + e.getMessage());
            return -1;
        }
    }

    private int insertOrder(Connection conn, int userId, String address, String paymentMethod)
            throws SQLException {
        String sql = "INSERT INTO DistributionOrders(user_id, order_date, delivery_address, payment_method, " +
                "total_amount, order_status) VALUES(?, ?, ?, ?, 0, ?)";
        try (PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, userId);
            p.setString(2, LocalDateTime.now().withNano(0).toString());
            p.setString(3, address);
            p.setString(4, paymentMethod);
            p.setString(5, PROCESSING);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create order");
    }

    // Deducts stock, inserts the order line, returns the subtotal
    private double addLine(Connection conn, int orderId, int itemId, int qty) throws SQLException {
        String name;
        double price;
        try (PreparedStatement p = conn.prepareStatement(
                "SELECT item_name, price FROM FurnitureItems WHERE item_id = ?")) {
            p.setInt(1, itemId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new SQLException("Item " + itemId + " not found");
                name = rs.getString("item_name");
                price = rs.getDouble("price");
            }
        }

        try (PreparedStatement p = conn.prepareStatement(
                "UPDATE FurnitureItems SET stock_quantity = stock_quantity - ? " +
                        "WHERE item_id = ? AND stock_quantity >= ?")) {
            p.setInt(1, qty);
            p.setInt(2, itemId);
            p.setInt(3, qty);
            if (p.executeUpdate() == 0) throw new SQLException("Not enough stock for " + name);
        }

        double subtotal = price * qty;
        try (PreparedStatement p = conn.prepareStatement(
                "INSERT INTO OrderItems(order_id, item_id, quantity, subtotal) VALUES(?, ?, ?, ?)")) {
            p.setInt(1, orderId);
            p.setInt(2, itemId);
            p.setInt(3, qty);
            p.setDouble(4, subtotal);
            p.executeUpdate();
        }
        return subtotal;
    }

    // ---------------- Read ----------------

    public List<DistributionOrder> getAllOrders() {
        List<DistributionOrder> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, COALESCE(u.username, '?') AS username, o.order_date, " +
                "o.delivery_address, o.payment_method, o.total_amount, o.order_status " +
                "FROM DistributionOrders o LEFT JOIN Users u ON u.user_id = o.user_id " +
                "ORDER BY o.order_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new DistributionOrder(
                        rs.getInt("order_id"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("order_date"),
                        rs.getString("delivery_address"),
                        rs.getString("payment_method"),
                        rs.getDouble("total_amount"),
                        rs.getString("order_status")));
            }
        } catch (SQLException e) {
            System.err.println("Could not load orders: " + e.getMessage());
        }
        return list;
    }

    public List<OrderLine> getLines(int orderId) {
        List<OrderLine> list = new ArrayList<>();
        String sql = "SELECT oi.order_item_id, oi.order_id, oi.item_id, f.item_name, oi.quantity, oi.subtotal " +
                "FROM OrderItems oi JOIN FurnitureItems f ON f.item_id = oi.item_id " +
                "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, orderId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderLine(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("subtotal")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load order lines: " + e.getMessage());
        }
        return list;
    }

    // ---------------- Update status ----------------

    // Returns null on success, otherwise an error message to show the user.
    // Allowed: Processing -> Shipped/Cancelled, Shipped -> Delivered/Cancelled.
    // Cancelling puts the stock back. Delivered and Cancelled are final.
    public String updateStatus(int orderId, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current = currentStatus(conn, orderId);
                if (current == null) {
                    conn.rollback();
                    return "Order not found.";
                }
                if (!isAllowed(current, newStatus)) {
                    conn.rollback();
                    return "Cannot change an order from " + current + " to " + newStatus + ".";
                }
                if (CANCELLED.equals(newStatus)) restock(conn, orderId);

                try (PreparedStatement p = conn.prepareStatement(
                        "UPDATE DistributionOrders SET order_status = ? WHERE order_id = ?")) {
                    p.setString(1, newStatus);
                    p.setInt(2, orderId);
                    p.executeUpdate();
                }
                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                return "Update failed: " + e.getMessage();
            }
        } catch (SQLException e) {
            return "Update failed: " + e.getMessage();
        }
    }

    private String currentStatus(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement(
                "SELECT order_status FROM DistributionOrders WHERE order_id = ?")) {
            p.setInt(1, orderId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private boolean isAllowed(String from, String to) {
        return switch (from) {
            case PROCESSING -> to.equals(SHIPPED) || to.equals(CANCELLED);
            case SHIPPED -> to.equals(DELIVERED) || to.equals(CANCELLED);
            default -> false;
        };
    }

    private void restock(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT item_id, quantity FROM OrderItems WHERE order_id = ?");
             PreparedStatement upd = conn.prepareStatement(
                     "UPDATE FurnitureItems SET stock_quantity = stock_quantity + ? WHERE item_id = ?")) {
            sel.setInt(1, orderId);
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    upd.setInt(1, rs.getInt("quantity"));
                    upd.setInt(2, rs.getInt("item_id"));
                    upd.executeUpdate();
                }
            }
        }
    }
}