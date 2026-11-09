package com.scc.dao;

import java.sql.*;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:furnitureRecord.db";

    private static boolean tablesReady = false;

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);

        // SQLite ignores foreign keys unless this is turned on for every connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        if (!tablesReady) {
            DatabaseConnection.createTables(conn);
            tablesReady = true;
            System.out.println("[DB] Using: " + new java.io.File("furnitureRecord.db").getAbsolutePath());
        }
        return conn;
    }

    public static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS Users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "full_name TEXT NOT NULL, " +
                    "roles TEXT NOT NULL, " +
                    "status TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS FurnitureItems (" +
                    "item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "item_name TEXT NOT NULL, " +
                    "category TEXT, " +
                    "dimensions TEXT, " +
                    "material TEXT, " +
                    "price REAL NOT NULL DEFAULT 0, " +
                    "stock_quantity INTEGER NOT NULL DEFAULT 0, " +
                    "warehouse_location TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS DistributionOrders (" +
                    "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "order_date TEXT NOT NULL, " +
                    "delivery_address TEXT, " +
                    "payment_method TEXT NOT NULL DEFAULT 'Cash on Delivery', " +
                    "total_amount REAL NOT NULL DEFAULT 0, " +
                    "order_status TEXT NOT NULL DEFAULT 'Processing', " +
                    "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)");

            // Older databases were created without payment_method, so add it if it is missing
            migrateOrderPayment(stmt);

            stmt.execute("CREATE TABLE IF NOT EXISTS OrderItems (" +
                    "order_item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "order_id INTEGER NOT NULL, " +
                    "item_id INTEGER NOT NULL, " +
                    "quantity INTEGER NOT NULL, " +
                    "subtotal REAL NOT NULL, " +
                    "FOREIGN KEY (order_id) REFERENCES DistributionOrders(order_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (item_id) REFERENCES FurnitureItems(item_id) ON DELETE CASCADE)");

            migrateActivityLogs(stmt);

            stmt.execute("CREATE TABLE IF NOT EXISTS ActivityLogs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "action_type TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "timestamp TEXT NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)");
        }
    }

    // Adds payment_method to an existing DistributionOrders table. Old orders get 'Cash on Delivery'.
    private static void migrateOrderPayment(Statement stmt) throws SQLException {
        boolean hasColumn = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(DistributionOrders)")) {
            while (rs.next()) {
                if ("payment_method".equalsIgnoreCase(rs.getString("name"))) {
                    hasColumn = true;
                }
            }
        }
        if (!hasColumn) {
            stmt.execute("ALTER TABLE DistributionOrders " +
                    "ADD COLUMN payment_method TEXT NOT NULL DEFAULT 'Cash on Delivery'");
        }
    }

    // Drops the old-format ActivityLogs table (log_id/event_type/target_id) so it can be recreated
    private static void migrateActivityLogs(Statement stmt) throws SQLException {
        boolean hasNewSchema = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(ActivityLogs)")) {
            while (rs.next()) {
                if ("action_type".equalsIgnoreCase(rs.getString("name"))) {
                    hasNewSchema = true;
                }
            }
        }
        if (!hasNewSchema) {
            stmt.execute("DROP TABLE IF EXISTS ActivityLogs");
        }
    }
}