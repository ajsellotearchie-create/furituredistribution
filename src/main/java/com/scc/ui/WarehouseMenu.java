package com.scc.ui;

import com.scc.model.User;

import java.util.Scanner;

public class WarehouseMenu {

    private final User user;
    private final Scanner sc;

    public WarehouseMenu(User user, Scanner sc) {
        this.user = user;
        this.sc = sc;
    }

    // Runs until the staff member logs out, then returns to the caller (Main)
    public void show() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("=============================");
            System.out.println("WAREHOUSE DASHBOARD  (user: " + user.username() + ")");
            System.out.println("=============================");
            System.out.println("1. View Inventory");
            System.out.println("2. Update Stock");
            System.out.println("3. Log out");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("[View Inventory] - coming soon\n");
                    break;
                case "2":
                    System.out.println("[Update Stock] - coming soon\n");
                    break;
                case "3":
                    System.out.println("Logged out successfully.\n");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.\n");
            }
        }
    }
}