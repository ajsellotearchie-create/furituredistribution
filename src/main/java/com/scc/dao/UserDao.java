package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public boolean registerUser(String username, String password, String fullName, String role) {
        // Check if the role is admin; if so, set status to 'active', otherwise 'pending'
        String status = role.equalsIgnoreCase("admin") ? "active" : "pending";

        String sql = "INSERT INTO Users(username, password, full_name, roles, status) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role.toLowerCase());
            pstmt.setString(5, status);
            pstmt.executeUpdate();

            if (status.equalsIgnoreCase("active")) {
                System.out.println("Registration successful. Your Admin account is active and ready to log in.");
            } else {
                System.out.println("Registration successful. Your account is pending admin approval.");
            }
            return true;

        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    // Returns the User if credentials match and account is approved/active, otherwise null
    public User login(String username, String password) {
        String sql = "SELECT user_id, username, password, full_name, roles, status " +
                "FROM Users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    // Check if account is still pending approval
                    if (status != null && status.equalsIgnoreCase("pending")) {
                        System.out.println("Login failed: Your account is pending admin approval.\n");
                        return null;
                    }

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("full_name"),
                            rs.getString("roles"),
                            status);
                }
            }
        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
        }
        return null;
    }
}