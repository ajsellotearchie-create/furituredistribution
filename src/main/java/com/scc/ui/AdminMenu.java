package com.scc.ui;

import com.scc.dao.*;
import com.scc.model.ActivityLog;
import com.scc.model.FurnitureItem;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final User user;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final FurnitureDao furnitureDao = new FurnitureDao();
    private final UserAdminDao userAdminDao = new UserAdminDao();
    private final UserDao userDao = new UserDao();
    private final ReportDao reportDao = new ReportDao();

    // The Scanner is passed in so the whole app shares one Scanner on System.in
    public AdminMenu(User user, Scanner sc) {
        this.user = user;
        this.sc = sc;
    }

    public void show() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("=============================");
            System.out.println("ADMIN DASHBOARD  (user: " + user.username() + ")");
            System.out.println("=============================");
            System.out.println("1. Manage Furniture");
            System.out.println("2. Manage Users");
            System.out.println("3. View Reports");
            System.out.println("4. View System Audit Logs");
            System.out.println("5. Log out");
            System.out.print("Choose an option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> manageFurniture();
                case "2" -> {
                    if (manageUsers()) {
                        loggedIn = false;
                    }
                }
                case "3" -> viewReports();
                case "4" -> viewAuditLogs();
                case "5" -> {
                    int id = 0;
                    logDao.logAction(user.user_id(), "LOGOUT",
                            "User " + user.username() + " (ID: " + user.user_id() + ") logged out.");
                    System.out.println("Logged out successfully.\n");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    // ---------------- Audit Logs ----------------

    private void viewAuditLogs() {
        boolean back = false;
        while (!back) {
            System.out.println("=============================");
            System.out.println("SYSTEM AUDIT LOGS");
            System.out.println("1. View all system logs");
            System.out.println("2. Filter logs by User ID");
            System.out.println("3. Back");
            System.out.println("=============================");
            System.out.print("Choose: ");
            switch (sc.nextLine().trim()) {
                case "1" -> printLogs(logDao.getAllLogs());
                case "2" -> printLogs(logDao.getLogsByUserId(readInt("User ID: ")));
                case "3" -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println();
    }

    private void printLogs(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No logs found.\n");
            return;
        }
        String fmt = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        System.out.printf(fmt, "ID", "User ID", "Action", "Description", "Timestamp");
        System.out.println("-".repeat(98));
        for (ActivityLog l : logs) {
            System.out.printf(fmt, l.id(), l.userId(), l.actionType(),
                    truncate(l.description(), 35), l.timestamp());
        }
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    // ---------------- Manage Furniture ----------------

    private void manageFurniture() {
        boolean back = false;
        while (!back) {
            System.out.println("==============================");
            System.out.println("1. View all ");
            System.out.println("2. Add ");
            System.out.println("3. Edit");
            System.out.println("4. Delete");
            System.out.println("5. Back");
            System.out.println("==============================");
            System.out.print("Choose: ");
            switch (sc.nextLine().trim()) {
                case "1" -> printItems(furnitureDao.getAll());
                case "2" -> addItem();
                case "3" -> editItem();
                case "4" -> deleteItem();
                case "5" -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println();
    }

    private void addItem() {
        String name = readText("Name: ", false);
        String category = readText("Category: ", true);
        String dimensions = readText("Dimensions (e.g. 120x60x75 cm): ", true);
        String material = readText("Material: ", true);
        double price = readDouble("Price: ");
        int stock = readInt("Initial stock: ");
        String location = readText("Warehouse location: ", true);

        int id = furnitureDao.addItem(new FurnitureItem(0, name, category, dimensions, material,
                price, stock, location));
        if (id > 0) {
            logDao.logAction(user.user_id(), "FURNITURE_ADDED",
                    "Admin " + user.user_id() + " added " + name + " (ID " + id + ", stock " + stock + ")");

            System.out.println("Item added with ID " + id + ".");
        } else {
            System.out.println("Could not add item.");
        }
    }

    private void editItem() {
        int id = readInt("Item ID to edit: ");
        FurnitureItem old = furnitureDao.findById(id);
        if (old == null) {
            System.out.println("Item not found.");
            return;
        }
        System.out.println("Press Enter to keep the current value.");
        String name = readOrKeep("Name", old.itemName());
        String category = readOrKeep("Category", old.category());
        String dimensions = readOrKeep("Dimensions", old.dimensions());
        String material = readOrKeep("Material", old.material());
        String priceText = readOrKeep("Price", String.valueOf(old.price()));
        String location = readOrKeep("Warehouse location", old.warehouseLocation());

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            System.out.println("Invalid price. Nothing changed.");
            return;
        }

        FurnitureItem updated = new FurnitureItem(id, name, category, dimensions, material,
                price, old.stockQuantity(), location);
        if (furnitureDao.updateItem(updated)) {
            logDao.logAction(user.user_id(), "FURNITURE_UPDATED",
                    "Admin " + user.user_id() + " edited " + name + " (ID " + id + ")");
            System.out.println("Item updated.");
        } else {
            System.out.println("Update failed.");
        }
    }

    private void deleteItem() {
        int id = readInt("Item ID to delete: ");
        FurnitureItem item = furnitureDao.findById(id);
        if (item == null) {
            System.out.println("Item not found.");
            return;
        }
        System.out.print("Delete \"" + item.itemName() + "\"? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Cancelled.");
            return;
        }
        if (furnitureDao.deleteItem(id)) {
            logDao.logAction(user.user_id(), "FURNITURE_DELETED",
                    "Admin " + user.user_id() + " deleted " + item.itemName() + " (ID " + id + ")");

            System.out.println("Item deleted.");
        }
    }

    // ---------------- Manage Users ----------------

    private boolean manageUsers() {
        boolean back = false;
        while (!back) {
            System.out.println("=============================");
            System.out.println("1. View all users");
            System.out.println("2. Approve pending users");
            System.out.println("3. Update user role & status");
            System.out.println("4. Delete user");
            System.out.println("5. Back");
            System.out.println("=============================");
            System.out.print("Choose: ");

            switch (sc.nextLine().trim()) {
                case "1" -> printUsers();
                case "2" -> approvePendingUsers();
                case "3" -> updateUserRoleAndStatus();
                case "4" -> {
                    if (deleteUser()) {
                        return true; // Signal back to show() that the user deleted themselves and needs to log out
                    }
                }
                case "5" -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println();
        return false;
    }

    private void printUsers() {
        List<User> users = userAdminDao.getAllUsers();
        System.out.printf("%-5s %-15s %-25s %-12s %-10s%n", "ID", "Username", "Full name", "Role", "Status");
        for (User u : users) {
            System.out.printf("%-5d %-15s %-25s %-12s %-10s%n",
                    u.user_id(), u.username(), u.full_name(), u.roles(), nz(u.status()));
        }
    }

    private void approvePendingUsers() {
        List<User> users = userAdminDao.getAllUsers();
        List<User> pendingUsers = new ArrayList<>();
        for (User u : users) {
            if ("pending".equalsIgnoreCase(u.status())) {
                pendingUsers.add(u);
            }
        }

        if (pendingUsers.isEmpty()) {
            System.out.println("No pending users found.\n");
            return;
        }

        System.out.println("\n--- Pending Users ---");
        System.out.printf("%-5s %-15s %-25s %-12s%n", "ID", "Username", "Full name", "Role");
        for (User u : pendingUsers) {
            System.out.printf("%-5d %-15s %-25s %-12s%n", u.user_id(), u.username(), u.full_name(), u.roles());
        }

        int id = readIntInput("Enter User ID to approve (0 to cancel): ");
        if (id == 0) return;

        boolean found = pendingUsers.stream().anyMatch(u -> u.user_id() == id);
        if (!found) {
            System.out.println("Invalid User ID or user is not pending.\n");
            return;
        }

        if (userAdminDao.updateStatus(id, "approved")) {
            logDao.logAction(user.user_id(), "USER_APPROVED",
                    "Admin " + user.user_id() + " approved account for User ID " + id);
            System.out.println("User account approved successfully.\n");
        } else {
            System.out.println("Approval failed.\n");
        }
    }

    // Updates another user's role and status. Enter 0 at any step to cancel.
    private void updateUserRoleAndStatus() {
        printUsers();
        int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.\n");
            return;
        }

        // Self-edit restriction
        if (targetUserId == user.user_id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.\n");
            return;
        }
<<<<<<< HEAD
        if (userAdminDao.updateRole(id, role)) {
            logDao.logAction(user.user_id(), "USER_UPDATED",
                    "Admin " + user.user_id() + " changed role of User " + id + " to " + role.toUpperCase());

            System.out.println("Role updated successfully.\n");
=======

        boolean exists = userAdminDao.getAllUsers().stream()
                .anyMatch(u -> u.user_id() == targetUserId);
        if (!exists) {
            System.out.println("User not found.\n");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.\n");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.\n");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
            logDao.log(user.user_id(), "UPDATE_USER", targetUserId,
                    "Set user #" + targetUserId + " to role " + newRole + ", status " + newStatus);
            System.out.println("User #" + targetUserId + " updated successfully: role=" + newRole
                    + ", status=" + newStatus + ".\n");
>>>>>>> 166ece63658bcdc4312e309df1254b943da17aab
        } else {
            System.out.println("Update failed.\n");
        }
    }

    private boolean deleteUser() {
        int id = readIntInput("User ID to delete (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.\n");
            return false;
        }

        System.out.print("Delete user #" + id + "? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Cancelled.\n");
            return false;
        }

        boolean isSelf = (id == user.user_id());

        if (userAdminDao.deleteUser(id)) {
            logDao.logAction(user.user_id(), "USER_DELETED",
                    "Admin " + user.user_id() + " deleted User ID " + id);
            System.out.println("User deleted successfully.\n");

            if (isSelf) {
                System.out.println("You have deleted your own account. Logging out...\n");
                return true; // Triggers logout flow
            }
        } else {
            System.out.println("User not found or cannot be deleted (user may have logs/orders).\n");
        }
        return false;
    }

    // ---------------- View Reports ----------------

    private void viewReports() {
        System.out.println("\n--- Inventory Summary ---");
        reportDao.inventorySummary().forEach(System.out::println);

        System.out.println("\n--- Low Stock (5 or fewer) ---");
        List<FurnitureItem> low = furnitureDao.getLowStock(5);
        if (low.isEmpty()) System.out.println("None.");
        else printItems(low);

        System.out.println("\n--- Orders by Status ---");
        reportDao.ordersByStatus().forEach(System.out::println);

        System.out.println("\n--- Recent Activity (last 15) ---");
        List<ActivityLog> recent = logDao.getAllLogs();
        if (recent.isEmpty()) System.out.println("No activity yet.");
        else printLogs(recent.subList(0, Math.min(15, recent.size())));
        System.out.println();
    }

    // ---------------- Helpers ----------------

    private void printItems(List<FurnitureItem> items) {
        if (items.isEmpty()) {
            System.out.println("No furniture found.");
            return;
        }
        System.out.printf("%-5s %-22s %-12s %-10s %-6s %-12s%n",
                "ID", "Name", "Category", "Price", "Stock", "Location");
        for (FurnitureItem i : items) {
            System.out.printf("%-5d %-22s %-12s %-10.2f %-6d %-12s%n",
                    i.itemId(), i.itemName(), nz(i.category()), i.price(),
                    i.stockQuantity(), nz(i.warehouseLocation()));
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private String readText(String prompt, boolean optional) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty() || optional) return s;
            System.out.println("This field is required.");
        }
    }

    private String readOrKeep(String label, String current) {
        System.out.print(label + " [" + nz(current) + "]: ");
        String s = sc.nextLine().trim();
        return s.isEmpty() ? current : s;
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    // Validation wrapper: keeps asking until a whole number is entered (no crash on letters/symbols)
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    // Validation wrapper: returns ADMIN or WAREHOUSE, or null if the admin types 0 to cancel
    private String readValidRole() {
        while (true) {
            System.out.print("New role (ADMIN / WAREHOUSE, 0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (input.equals("ADMIN") || input.equals("WAREHOUSE")) return input;
            System.out.println("Invalid role. Valid roles: ADMIN, WAREHOUSE.");
        }
    }

    // Validation wrapper: returns APPROVED or PENDING, or null if the admin types 0 to cancel
    private String readValidStatus() {
        while (true) {
            System.out.print("New status (APPROVED / PENDING, 0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (input.equals("APPROVED") || input.equals("PENDING")) return input;
            System.out.println("Invalid status. Valid statuses: APPROVED, PENDING.");
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double d = Double.parseDouble(sc.nextLine().trim());
                if (d >= 0) return d;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Please enter a valid non-negative number. ");
        }
    }
}