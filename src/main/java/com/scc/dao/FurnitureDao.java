package com.scc.dao;

import com.scc.model.FurnitureItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FurnitureDao {

    private static FurnitureItem map(ResultSet rs) throws SQLException {
        return new FurnitureItem(
                rs.getInt("item_id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("dimensions"),
                rs.getString("material"),
                rs.getDouble("price"),
                rs.getInt("stock_quantity"),
                rs.getString("warehouse_location"));
    }

    // Returns the new item_id, or -1 on failure
    public int addItem(FurnitureItem i) {
        String sql = "INSERT INTO FurnitureItems(item_name, category, dimensions, material, price, " +
                "stock_quantity, warehouse_location) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, i.itemName());
            p.setString(2, i.category());
            p.setString(3, i.dimensions());
            p.setString(4, i.material());
            p.setDouble(5, i.price());
            p.setInt(6, i.stockQuantity());
            p.setString(7, i.warehouseLocation());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Add item failed: " + e.getMessage());
        }
        return -1;
    }

    public List<FurnitureItem> getAll() {
        return query("SELECT * FROM FurnitureItems ORDER BY item_id", null);
    }

    public List<FurnitureItem> getLowStock(int threshold) {
        return query("SELECT * FROM FurnitureItems WHERE stock_quantity <= ? ORDER BY stock_quantity", threshold);
    }

    public FurnitureItem findById(int id) {
        List<FurnitureItem> list = query("SELECT * FROM FurnitureItems WHERE item_id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<FurnitureItem> query(String sql, Integer param) {
        List<FurnitureItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            if (param != null) p.setInt(1, param);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateItem(FurnitureItem i) {
        String sql = "UPDATE FurnitureItems SET item_name = ?, category = ?, dimensions = ?, material = ?, " +
                "price = ?, warehouse_location = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, i.itemName());
            p.setString(2, i.category());
            p.setString(3, i.dimensions());
            p.setString(4, i.material());
            p.setDouble(5, i.price());
            p.setString(6, i.warehouseLocation());
            p.setInt(7, i.itemId());
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
            return false;
        }
    }

    // delta can be positive (stock in) or negative (stock out). Refuses to go below zero.
    public boolean adjustStock(int itemId, int delta) {
        String sql = "UPDATE FurnitureItems SET stock_quantity = stock_quantity + ? " +
                "WHERE item_id = ? AND stock_quantity + ? >= 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, delta);
            p.setInt(2, itemId);
            p.setInt(3, delta);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Stock update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteItem(int itemId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement("DELETE FROM FurnitureItems WHERE item_id = ?")) {
            p.setInt(1, itemId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            // Usually a foreign key error: the item is already part of an order
            System.err.println("Delete failed (item may be used in an order): " + e.getMessage());
            return false;
        }
    }
}