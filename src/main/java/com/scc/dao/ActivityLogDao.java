package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ActivityLogDao {

    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO ActivityLogs(user_id, action_type, description, timestamp) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            p.setString(2, actionType);
            p.setString(3, description);
            p.setString(4, LocalDateTime.now().withNano(0).toString());
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Logging failed: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        String sql = "SELECT id, user_id, action_type, description, timestamp " +
                "FROM ActivityLogs ORDER BY timestamp DESC, id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                logs.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Could not load logs: " + e.getMessage());
        }
        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = "SELECT id, user_id, action_type, description, timestamp " +
                "FROM ActivityLogs WHERE user_id = ? ORDER BY timestamp DESC, id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load logs: " + e.getMessage());
        }
        return logs;
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp"));
    }
}