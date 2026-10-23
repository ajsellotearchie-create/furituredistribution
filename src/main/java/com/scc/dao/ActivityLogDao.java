package com.scc.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean log(int userId, String eventType, int targetId, String description) {
        String sql = "INSERT INTO ActivityLogs(user_id, event_type, target_id, description, timestamp) " +
                "VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            p.setString(2, eventType);
            p.setInt(3, targetId);
            p.setString(4, description);
            p.setString(5, LocalDateTime.now().withNano(0).toString());
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Logging failed: " + e.getMessage());
            return false;
        }
    }

    // Newest first, formatted as ready-to-print lines
    public List<String> getRecent(int limit) {
        String sql = "SELECT l.timestamp, u.username, l.event_type, l.target_id, l.description " +
                "FROM ActivityLogs l JOIN Users u ON u.user_id = l.user_id " +
                "ORDER BY l.log_id DESC LIMIT ?";
        List<String> lines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    lines.add(String.format("%-20s %-12s %-16s #%-4d %s",
                            rs.getString("timestamp"), rs.getString("username"),
                            rs.getString("event_type"), rs.getInt("target_id"),
                            rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load logs: " + e.getMessage());
        }
        return lines;
    }
}