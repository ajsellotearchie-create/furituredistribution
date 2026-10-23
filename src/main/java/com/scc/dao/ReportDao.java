package com.scc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    public List<String> inventorySummary() {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT COUNT(*) AS items, COALESCE(SUM(stock_quantity), 0) AS units, " +
                "COALESCE(SUM(price * stock_quantity), 0) AS value FROM FurnitureItems";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                lines.add("Furniture types : " + rs.getInt("items"));
                lines.add("Units in stock  : " + rs.getInt("units"));
                lines.add(String.format("Inventory value : %,.2f", rs.getDouble("value")));
            }
        } catch (SQLException e) {
            System.err.println("Report failed: " + e.getMessage());
        }
        return lines;
    }

    public List<String> ordersByStatus() {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT order_status, COUNT(*) AS n, COALESCE(SUM(total_amount), 0) AS total " +
                "FROM DistributionOrders GROUP BY order_status";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                lines.add(String.format("%-12s orders: %-4d total: %,.2f",
                        rs.getString("order_status"), rs.getInt("n"), rs.getDouble("total")));
            }
        } catch (SQLException e) {
            System.err.println("Report failed: " + e.getMessage());
        }
        if (lines.isEmpty()) lines.add("No orders yet.");
        return lines;
    }
}