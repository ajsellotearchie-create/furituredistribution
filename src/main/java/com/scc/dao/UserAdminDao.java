package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Admin-only user operations
public class UserAdminDao {

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, password, full_name, roles, status FROM Users ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("roles"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            System.err.println("Could not load users: " + e.getMessage());
        }
        return list;
    }

    public boolean updateRole(int userId, String role) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement("UPDATE Users SET roles = ? WHERE user_id = ?")) {
            p.setString(1, role.toLowerCase());
            p.setInt(2, userId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update role failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int userId, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement("UPDATE Users SET status = ? WHERE user_id = ?")) {
            p.setString(1, status.toLowerCase());
            p.setInt(2, userId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update status failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement("DELETE FROM Users WHERE user_id = ?")) {
            p.setInt(1, userId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Delete failed: " + e.getMessage());
            return false;
        }
    }
}