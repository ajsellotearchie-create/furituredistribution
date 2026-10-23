package com.scc.ui;

import com.scc.dao.*;
import com.scc.model.FurnitureItem;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final User user;
    private final Scanner sc;
    private final FurnitureDao furnitureDao = new FurnitureDao();
    private final UserAdminDao userAdminDao = new UserAdminDao();
    private final ReportDao reportDao = new ReportDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

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
            System.out.println("4. Log out");
            System.out.print("Choose an option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> manageFurniture();
                case "2" -> {
                    // If manageUsers returns true, it means the admin deleted their own account and we need to log out
                    if (manageUsers()) {
                        loggedIn = false;
                    }
                }
                case "3" -> viewReports();
                case "4" -> {
                    logDao.log(user.user_id(), "LOGOUT", user.user_id(), user.username() + " logged out");
                    System.out.println("Logged out successfully.\n");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Try again.\n");
            }
        }
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
            logDao.log(user.user_id(), "ADD_FURNITURE", id, "Added " + name + " (stock " + stock + ")");
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
            logDao.log(user.user_id(), "EDIT_FURNITURE", id, "Edited " + name);
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
            logDao.log(user.user_id(), "DELETE_FURNITURE", id, "Deleted " + item.itemName());
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
            System.out.println("3. Change role");
            System.out.println("4. Delete user");
            System.out.println("5. Back");
            System.out.println("=============================");
            System.out.print("Choose: ");

            switch (sc.nextLine().trim()) {
                case "1" -> printUsers();
                case "2" -> approvePendingUsers();
                case "3" -> changeRole();
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

        int id = readInt("Enter User ID to approve (0 to cancel): ");
        if (id == 0) return;

        boolean found = pendingUsers.stream().anyMatch(u -> u.user_id() == id);
        if (!found) {
            System.out.println("Invalid User ID or user is not pending.\n");
            return;
        }

        if (userAdminDao.updateStatus(id, "approved")) {
            logDao.log(user.user_id(), "APPROVE_USER", id, "Approved user account #" + id);
            System.out.println("User account approved successfully.\n");
        } else {
            System.out.println("Approval failed.\n");
        }
    }

    private void changeRole() {
        int id = readInt("User ID: ");
        if (id == user.user_id()) {
            System.out.println("You cannot change your own role.\n");
            return;
        }
        String role = readText("New role (admin / warehouse): ", false).toLowerCase();
        if (!role.equals("admin") && !role.equals("warehouse")) {
            System.out.println("Role must be admin or warehouse.\n");
            return;
        }
        if (userAdminDao.updateRole(id, role)) {
            logDao.log(user.user_id(), "CHANGE_ROLE", id, "Set role of user #" + id + " to " + role);
            System.out.println("Role updated successfully.\n");
        } else {
            System.out.println("User not found or update failed.\n");
        }
    }

    private boolean deleteUser() {
        int id = readInt("User ID to delete: ");

        System.out.print("Delete user #" + id + "? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Cancelled.\n");
            return false;
        }

        boolean isSelf = (id == user.user_id());

        if (userAdminDao.deleteUser(id)) {
            logDao.log(user.user_id(), "DELETE_USER", id, "Deleted user #" + id);
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
        List<String> logs = logDao.getRecent(15);
        if (logs.isEmpty()) System.out.println("No activity yet.");
        else logs.forEach(System.out::println);
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