package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.FurnitureDao;
import com.scc.model.FurnitureItem;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

import static com.scc.ui.InputHelper.*;

public class WarehouseMenu {

    private static final int LOW_STOCK_LIMIT = 10;

    private final User user;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final FurnitureDao furnitureDao = new FurnitureDao();

    public WarehouseMenu(User user, Scanner sc) {
        this.user = user;
        this.sc = sc;
    }

    public void show() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("=============================");
            System.out.println("WAREHOUSE DASHBOARD  (user: " + user.username() + ")");
            System.out.println("=============================");
            System.out.println("1. View Inventory");
            System.out.println("2. Update Stock");
            System.out.println("3. Low Stock Report");
            System.out.println("4. Log out");
            System.out.print("Choose an option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> printItems(furnitureDao.getAll());
                case "2" -> updateStock();
                case "3" -> {
                    System.out.println("\n--- Low Stock (" + LOW_STOCK_LIMIT + " or fewer) ---");
                    printItems(furnitureDao.getLowStock(LOW_STOCK_LIMIT));
                }
                case "4" -> {
                    logDao.logAction(user.user_id(), "LOGOUT",
                            "User " + user.username() + " (ID: " + user.user_id() + ") logged out.");
                    System.out.println("Logged out successfully.\n");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    private void updateStock() {
        int id = readInt(sc, "Item ID (0 to cancel): ");
        if (id == 0) return;

        FurnitureItem item = furnitureDao.findById(id);
        if (item == null) {
            System.out.println("Item not found.\n");
            return;
        }
        System.out.println(item.itemName() + " - current stock: " + item.stockQuantity());
        System.out.println("1. Stock in (add)");
        System.out.println("2. Stock out (remove)");
        System.out.println("0. Cancel");
        String choice = sc.nextLine().trim();
        if (choice.equals("0")) return;
        if (!choice.equals("1") && !choice.equals("2")) {
            System.out.println("Invalid option.\n");
            return;
        }

        int qty = readInt(sc, "Quantity: ");
        if (qty <= 0) {
            System.out.println("Quantity must be greater than zero.\n");
            return;
        }
        int delta = choice.equals("1") ? qty : -qty;

        if (furnitureDao.adjustStock(id, delta)) {
            FurnitureItem after = furnitureDao.findById(id);
            logDao.logAction(user.user_id(), "STOCK_ADJUSTED",
                    "User " + user.user_id() + " changed " + item.itemName() + " (ID " + id + ") by "
                            + (delta > 0 ? "+" : "") + delta + ", new stock " + after.stockQuantity());
            System.out.println("Stock updated. New stock: " + after.stockQuantity() + "\n");
        } else {
            System.out.println("Update failed. Stock cannot go below zero.\n");
        }
    }

    private void printItems(List<FurnitureItem> items) {
        if (items.isEmpty()) {
            System.out.println("No furniture found.\n");
            return;
        }
        System.out.printf("%-5s %-22s %-12s %-12s %-10s %-6s %-12s%n",
                "ID", "Name", "Category", "Material", "Price", "Stock", "Location");
        for (FurnitureItem i : items) {
            System.out.printf("%-5d %-22s %-12s %-12s %-10.2f %-6d %-12s%n",
                    i.itemId(), i.itemName(), nz(i.category()), nz(i.material()), i.price(),
                    i.stockQuantity(), nz(i.warehouseLocation()));
        }
        System.out.println();
    }
}