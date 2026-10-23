package com.scc;

import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;
import com.scc.ui.WarehouseMenu;

import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();

    public static void main(String[] args) {
        boolean running = true;

        // The menu loops, so after register (or a failed login) you return here.
        while (running) {
            showMainMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    login();
                    break;
                case "2":
                    register();
                    break; // back to the top of the loop = main menu
                case "3":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    private static void showMainMenu() {
        System.out.println("=============================");
        System.out.println("Furniture Management");
        System.out.println("=============================");
        System.out.println("1. Log in");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
    }

    // ---------------- LOG IN ----------------
    private static void login() {
        System.out.println("\n--- Log in ---");
        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        // UserDao.login returns a User if correct, otherwise null
        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println("Invalid username or password!\n");
            return; // back to main menu
        }

        System.out.println("Login successful! Welcome, " + user.full_name()
                + " (" + user.roles() + ")\n");

        // roles are stored in lowercase in the database, so ignore case
        if (user.roles().equalsIgnoreCase("admin")) {
            new AdminMenu(user, sc).show();
        } else {
            new WarehouseMenu(user, sc).show();
        }
        // when a dashboard returns (logout), we land back in the main menu loop
    }

    // ---------------- REGISTER ----------------
    private static void register() {
        System.out.println("\n--- Register ---");
        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String password = sc.nextLine();
        System.out.print("Enter full name: ");
        String fullName = sc.nextLine().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            System.out.println("All fields are required!\n");
            return;
        }

        System.out.print("Select role: ADMIN [1] | Warehouse Staff [2]: ");
        String roleChoice = sc.nextLine().trim();

        String role;
        switch (roleChoice) {
            case "1":
                role = "ADMIN";
                break;
            case "2":
                role = "Warehouse Staff";
                break;
            default:
                role = null;
        }

        if (role == null) {
            System.out.println("Invalid role!\n");
            return;
        }

        // registerUser prints its own success/failure message
        boolean success = userDao.registerUser(username, password, fullName, role);

        if (success) {
            System.out.println("Please log in with your new account.\n");
        } else {
            System.out.println("(Username may already exist.)\n");
        }
        // returns to the main menu loop automatically
    }
}