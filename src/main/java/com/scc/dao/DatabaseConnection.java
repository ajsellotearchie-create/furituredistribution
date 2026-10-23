package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
            createTables(conn);
            tablesReady = true;
        }
        return conn;
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS Users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "full_name TEXT NOT NULL, " +
                    "roles TEXT NOT NULL," +
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
                    "total_amount REAL NOT NULL DEFAULT 0, " +
                    "order_status TEXT NOT NULL DEFAULT 'Processing', " +
                    "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS OrderItems (" +
                    "order_item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "order_id INTEGER NOT NULL, " +
                    "item_id INTEGER NOT NULL, " +
                    "quantity INTEGER NOT NULL, " +
                    "subtotal REAL NOT NULL, " +
                    "FOREIGN KEY (order_id) REFERENCES DistributionOrders(order_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (item_id) REFERENCES FurnitureItems(item_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS ActivityLogs (" +
                    "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "event_type TEXT NOT NULL, " +
                    "target_id INTEGER NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "timestamp TEXT NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)");
        }
    }
}