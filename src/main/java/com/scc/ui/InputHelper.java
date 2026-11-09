package com.scc.ui;

import java.util.Scanner;

// Shared console input helpers so the menus don't crash on bad input
public final class InputHelper {

    private InputHelper() {
    }

    public static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    public static String readText(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("This field is required.");
        }
    }

    public static boolean confirm(Scanner sc, String prompt) {
        System.out.print(prompt + " (y/n): ");
        return sc.nextLine().trim().equalsIgnoreCase("y");
    }

    public static String nz(String s) {
        return s == null ? "" : s;
    }
}