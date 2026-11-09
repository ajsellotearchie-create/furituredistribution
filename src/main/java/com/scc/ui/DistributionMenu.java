package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.FurnitureDao;
import com.scc.dao.OrderDao;
import com.scc.model.DistributionOrder;
import com.scc.model.FurnitureItem;
import com.scc.model.OrderLine;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.scc.ui.InputHelper.*;

public class DistributionMenu {

    private final User user;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final FurnitureDao furnitureDao = new FurnitureDao();
    private final OrderDao orderDao = new OrderDao();

    public DistributionMenu(User user, Scanner sc) {
        this.user = user;
        this.sc = sc;
    }

    public void show() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("=============================");
            System.out.println("DISTRIBUTION DASHBOARD  (user: " + user.username() + ")");
            System.out.println("=============================");
            System.out.println("1. View Available Items");
            System.out.println("2. Distribute Item (Create Order)");
            System.out.println("3. View Distribution Orders");
            System.out.println("4. Update Order Status");
            System.out.println("5. Log out");
            System.out.print("Choose an option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> printItems(availableItems());
                case "2" -> createOrder();
                case "3" -> viewOrders();
                case "4" -> updateOrderStatus();
                case "5" -> {
                    logDao.logAction(user.user_id(), "LOGOUT",
                            "User " + user.username() + " (ID: " + user.user_id() + ") logged out.");
                    System.out.println("Logged out successfully.\n");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    // ---------------- Create order ----------------

    private void createOrder() {
        List<FurnitureItem> available = availableItems();
        if (available.isEmpty()) {
            System.out.println("Nothing in stock to distribute.\n");
            return;
        }
        printItems(available);

        Map<Integer, Integer> cart = new LinkedHashMap<>();
        while (true) {
            int id = readInt(sc, "Item ID to add (0 = done): ");
            if (id == 0) break;

            FurnitureItem item = furnitureDao.findById(id);
            if (item == null) {
                System.out.println("Item not found.");
                continue;
            }
            int free = item.stockQuantity() - cart.getOrDefault(id, 0);
            if (free <= 0) {
                System.out.println("No stock left for " + item.itemName() + ".");
                continue;
            }
            int qty = readInt(sc, "Quantity for " + item.itemName() + " (available " + free + "): ");
            if (qty <= 0 || qty > free) {
                System.out.println("Quantity must be between 1 and " + free + ".");
                continue;
            }
            cart.merge(id, qty, Integer::sum);
            System.out.println("Added. Different items in order: " + cart.size());
        }

        if (cart.isEmpty()) {
            System.out.println("No items selected. Order cancelled.\n");
            return;
        }

        // Summary
        System.out.println("\n--- Order Summary ---");
        double total = 0;
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            FurnitureItem item = furnitureDao.findById(e.getKey());
            double sub = item.price() * e.getValue();
            total += sub;
            System.out.printf("%-22s x%-4d %,12.2f%n", item.itemName(), e.getValue(), sub);
        }
        System.out.printf("%-27s %,12.2f%n", "TOTAL", total);

        String address = readText(sc, "Delivery address: ");
        String payment = choosePaymentMethod();
        if (payment == null) {
            System.out.println("Order cancelled.\n");
            return;
        }
        System.out.println("Payment method: " + payment);
        if (!confirm(sc, "Confirm order?")) {
            System.out.println("Order cancelled.\n");
            return;
        }

        int orderId = orderDao.createOrder(user.user_id(), address, payment, cart);
        if (orderId > 0) {
            logDao.logAction(user.user_id(), "ORDER_CREATED",
                    "User " + user.user_id() + " created order #" + orderId
                            + " (" + cart.size() + " item types, total " + String.format("%.2f", total)
                            + ", " + payment + ")");
            System.out.println("Order #" + orderId + " created. Stock has been deducted.\n");
        } else {
            System.out.println("Order could not be created. Nothing was changed.\n");
        }
    }

    // ---------------- View orders ----------------

    private void viewOrders() {
        List<DistributionOrder> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders yet.\n");
            return;
        }
        String fmt = "%-6s %-12s %-20s %-12s %-18s %-12s %-25s%n";
        System.out.printf(fmt, "ID", "By", "Date", "Total", "Payment", "Status", "Address");
        for (DistributionOrder o : orders) {
            System.out.printf(fmt, o.orderId(), o.username(), o.orderDate(),
                    String.format("%,.2f", o.totalAmount()), o.paymentMethod(), o.orderStatus(),
                    truncate(o.deliveryAddress(), 25));
        }

        int id = readInt(sc, "\nOrder ID for details (0 to go back): ");
        if (id == 0) return;

        DistributionOrder order = orders.stream().filter(o -> o.orderId() == id).findFirst().orElse(null);
        if (order == null) {
            System.out.println("Order not found.\n");
            return;
        }
        System.out.println("\nOrder #" + order.orderId() + " | " + order.orderStatus()
                + " | " + order.orderDate());
        System.out.println("Deliver to: " + nz(order.deliveryAddress()));
        System.out.println("Payment   : " + nz(order.paymentMethod()));
        for (OrderLine l : orderDao.getLines(id)) {
            System.out.printf("  %-22s x%-4d %,12.2f%n", l.itemName(), l.quantity(), l.subtotal());
        }
        System.out.printf("  %-27s %,12.2f%n%n", "TOTAL", order.totalAmount());
    }

    // ---------------- Update status ----------------

    private void updateOrderStatus() {
        List<DistributionOrder> open = new ArrayList<>();
        for (DistributionOrder o : orderDao.getAllOrders()) {
            if (o.orderStatus().equals(OrderDao.PROCESSING) || o.orderStatus().equals(OrderDao.SHIPPED)) {
                open.add(o);
            }
        }
        if (open.isEmpty()) {
            System.out.println("No open orders to update.\n");
            return;
        }
        System.out.printf("%-6s %-12s %-12s %-12s%n", "ID", "By", "Total", "Status");
        for (DistributionOrder o : open) {
            System.out.printf("%-6d %-12s %-12s %-12s%n", o.orderId(), o.username(),
                    String.format("%,.2f", o.totalAmount()), o.orderStatus());
        }

        int id = readInt(sc, "Order ID to update (0 to cancel): ");
        if (id == 0) return;
        if (open.stream().noneMatch(o -> o.orderId() == id)) {
            System.out.println("Order not found or already Delivered/Cancelled.\n");
            return;
        }

        System.out.println("1. Shipped");
        System.out.println("2. Delivered");
        System.out.println("3. Cancelled (returns stock to inventory)");
        System.out.println("0. Back");
        System.out.print("Choose: ");
        String newStatus = switch (sc.nextLine().trim()) {
            case "1" -> OrderDao.SHIPPED;
            case "2" -> OrderDao.DELIVERED;
            case "3" -> OrderDao.CANCELLED;
            default -> null;
        };
        if (newStatus == null) {
            System.out.println("Cancelled.\n");
            return;
        }
        if (newStatus.equals(OrderDao.CANCELLED) && !confirm(sc, "Cancel order #" + id + " and restock items?")) {
            System.out.println("Cancelled.\n");
            return;
        }

        String error = orderDao.updateStatus(id, newStatus);
        if (error == null) {
            logDao.logAction(user.user_id(), "ORDER_STATUS_UPDATED",
                    "User " + user.user_id() + " set order #" + id + " to " + newStatus);
            System.out.println("Order #" + id + " is now " + newStatus + ".\n");
        } else {
            System.out.println(error + "\n");
        }
    }

    // ---------------- Helpers ----------------

    // Returns the chosen payment method, or null if the user cancels
    private String choosePaymentMethod() {
        List<String> methods = OrderDao.PAYMENT_METHODS;
        System.out.println("\nPayment method:");
        for (int i = 0; i < methods.size(); i++) {
            System.out.println((i + 1) + ". " + methods.get(i));
        }
        System.out.println("0. Cancel order");
        while (true) {
            int choice = readInt(sc, "Choose: ");
            if (choice == 0) return null;
            if (choice >= 1 && choice <= methods.size()) return methods.get(choice - 1);
            System.out.println("Invalid option.");
        }
    }

    private List<FurnitureItem> availableItems() {
        List<FurnitureItem> list = new ArrayList<>();
        for (FurnitureItem i : furnitureDao.getAll()) {
            if (i.stockQuantity() > 0) list.add(i);
        }
        return list;
    }

    private void printItems(List<FurnitureItem> items) {
        if (items.isEmpty()) {
            System.out.println("No items available.\n");
            return;
        }
        System.out.printf("%-5s %-22s %-12s %-10s %-6s%n", "ID", "Name", "Category", "Price", "Stock");
        for (FurnitureItem i : items) {
            System.out.printf("%-5d %-22s %-12s %-10.2f %-6d%n",
                    i.itemId(), i.itemName(), nz(i.category()), i.price(), i.stockQuantity());
        }
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}