package org.core;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PropertyManager {
    private static final String[] BUILDINGS = {
            "property_1",
            "property_2",
            "property_3",
            "property_4",
            "property_5"
    };

    public static void start() {
        Scanner scanner = new Scanner(System.in);

        for (String building : BUILDINGS) {
            DatabaseManager.initializeDatabase(building);
        }

        boolean exit = false;

        while (!exit) {
            System.out.println("\nSelect a building to manage (or type 'back' to exit):");
            for (int i = 0; i < BUILDINGS.length; i++) {
                System.out.println((i + 1) + ". " + BUILDINGS[i]);
            }

            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("back")) {
                exit = true;
            } else {
                try {
                    int choice = Integer.parseInt(input);
                    if (choice >= 1 && choice <= BUILDINGS.length) {
                        manageBuilding(BUILDINGS[choice - 1], scanner);
                    } else {
                        System.out.println("Invalid choice. Try again.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Try again.");
                }
            }
        }
        scanner.close();
    }

    private static void manageBuilding(String dbName, Scanner scanner) {
        boolean back = false;

        while (!back) {
            System.out.println("\nManaging " + dbName + " Database:");
            System.out.println("1. View Tenants");
            System.out.println("2. Add Tenant");
            System.out.println("3. Record Monthly Payment");
            System.out.println("4. Delete Tenant");
            System.out.println("5. Increase Rent for a Tenant");
            System.out.println("6. Edit Tenant Information");
            System.out.println("7. Go Back");

            System.out.print("Enter your choice: ");
            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    viewTenants(dbName);
                    break;
                case "2":
                    addTenant(dbName, scanner);
                    break;
                case "3":
                    recordPayment(dbName, scanner);
                    break;
                case "4":
                    deleteTenant(dbName, scanner);
                    break;
                case "5":
                    increaseRent(dbName, scanner);
                    break;
                case "6":  // ✅ Edit Tenant Information now works correctly
                    editTenant(dbName, scanner);
                    break;
                case "7":  // ✅ Correctly allows the user to go back
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a valid number (1-7).");
            }
        }
    }


    private static void viewTenants(String dbName) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tenants")) {

            System.out.println("\nTenants in " + dbName + ":");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        ", Name: " + rs.getString("name") +
                        ", Apt#: " + rs.getString("apt_number") +
                        ", Lease Start: " + rs.getDate("lease_start") +
                        ", Lease Expiry: " + rs.getDate("lease_expired") +
                        ", Security: $" + rs.getDouble("security") +
                        ", Rent: $" + rs.getDouble("rent") +
                        ", Balance: $" + rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addTenant(String dbName, Scanner scanner) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "")) {
            System.out.println("Enter tenant details (or type 'back' to return):");

            System.out.print("Enter tenant name: ");
            String name = scanner.nextLine();
            if (name.equalsIgnoreCase("back")) return;

            System.out.print("Enter apartment number: ");
            String aptNumber = scanner.nextLine();
            if (aptNumber.equalsIgnoreCase("back")) return;

            System.out.print("Enter lease start (MM-DD-YYYY): ");
            String leaseStart = scanner.nextLine();
            if (leaseStart.equalsIgnoreCase("back")) return;
            leaseStart = convertDateFormat(leaseStart);

            System.out.print("Enter lease expiry (MM-DD-YYYY): ");
            String leaseExpired = scanner.nextLine();
            if (leaseExpired.equalsIgnoreCase("back")) return;
            leaseExpired = convertDateFormat(leaseExpired);

            System.out.print("Enter security deposit: ");
            double security = Double.parseDouble(scanner.nextLine());

            System.out.print("Enter monthly rent: ");
            double rent = Double.parseDouble(scanner.nextLine());

            // Calculate initial balance based on the number of unpaid months
            int unpaidMonths = getUnpaidMonths(leaseStart);
            double balance = rent * unpaidMonths;

            String sql = "INSERT INTO tenants (name, apt_number, lease_start, lease_expired, security, rent, balance) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, aptNumber);
                pstmt.setDate(3, Date.valueOf(leaseStart));
                pstmt.setDate(4, Date.valueOf(leaseExpired));
                pstmt.setDouble(5, security);
                pstmt.setDouble(6, rent);
                pstmt.setDouble(7, balance);
                pstmt.executeUpdate();
                System.out.println("Tenant added successfully with an initial balance of $" + balance);
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Invalid input. Please try again.");
        }
    }




    private static void recordPayment(String dbName, Scanner scanner) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "")) {
            // Fetch tenant list for selection
            List<Integer> tenantIds = new ArrayList<>();
            List<String> tenantNames = new ArrayList<>();

            System.out.println("\nSelect a tenant to record a payment:");

            String selectAllSql = "SELECT id, name, apt_number FROM tenants";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectAllSql)) {

                int index = 1;
                while (rs.next()) {
                    tenantIds.add(rs.getInt("id"));
                    tenantNames.add(rs.getString("name") + " (Apt#: " + rs.getString("apt_number") + ")");
                    System.out.println(index + ". " + tenantNames.get(index - 1));
                    index++;
                }

                if (tenantIds.isEmpty()) {
                    System.out.println("No tenants found.");
                    return;
                }

                System.out.print("\nEnter the number of the tenant to record a payment (or type 'back' to cancel): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("back")) return;

                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice. Please enter a valid number.");
                    return;
                }

                if (choice < 1 || choice > tenantIds.size()) {
                    System.out.println("Invalid choice. Try again.");
                    return;
                }

                int tenantId = tenantIds.get(choice - 1);

                // Ask for the month
                System.out.print("Enter month (e.g., 'jan', 'feb', 'mar'): ");
                String month = scanner.nextLine().toLowerCase();

                // Validate that the month is correct
                List<String> validMonths = Arrays.asList("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");
                if (!validMonths.contains(month)) {
                    System.out.println("Invalid month entered. Try again.");
                    return;
                }

                String monthColumn = month + "_paid";

                System.out.print("Enter payment amount: ");
                double payment;
                try {
                    String paymentInput = scanner.nextLine();
                    payment = Double.parseDouble(paymentInput.trim()); // Ensuring valid parsing
                } catch (NumberFormatException e) {
                    System.out.println("Invalid amount entered. Please enter a valid number.");
                    return;
                }

                // Fetch current rent and balance
                String checkSql = "SELECT rent, balance, " + monthColumn + " FROM tenants WHERE id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, tenantId);
                    ResultSet rsCheck = checkStmt.executeQuery();

                    if (rsCheck.next()) {
                        double rent = rsCheck.getDouble("rent");
                        double currentBalance = rsCheck.getDouble("balance");
                        double previousPayment = rsCheck.getDouble(monthColumn);

                        // Calculate new balance
                        double newBalance = currentBalance - payment;

                        // Update the payment for the given month and adjust balance
                        String sql = "UPDATE tenants SET " + monthColumn + " = ?, balance = ? WHERE id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setDouble(1, previousPayment + payment);
                            pstmt.setDouble(2, newBalance);
                            pstmt.setInt(3, tenantId);
                            pstmt.executeUpdate();

                            System.out.printf("Payment of $%.2f recorded for %s. New balance: $%.2f\n", payment, month.toUpperCase(), newBalance);
                        }
                    } else {
                        System.out.println("Error: Tenant not found.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error occurred.");
            e.printStackTrace();
        }
    }





    private static void deleteTenant(String dbName, Scanner scanner) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, apt_number FROM tenants")) {

            List<Integer> tenantIds = new ArrayList<>();
            List<String> tenantNames = new ArrayList<>();

            System.out.println("\nSelect a tenant to delete:");
            int index = 1;
            while (rs.next()) {
                tenantIds.add(rs.getInt("id"));
                tenantNames.add(rs.getString("name") + " (Apt#: " + rs.getString("apt_number") + ")");
                System.out.println(index + ". " + tenantNames.get(index - 1));
                index++;
            }

            if (tenantIds.isEmpty()) {
                System.out.println("No tenants found.");
                return;
            }

            System.out.print("\nEnter the number of the tenant to delete (or type 'back' to return): ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("back")) return;

            try {
                int choice = Integer.parseInt(input);
                if (choice < 1 || choice > tenantIds.size()) {
                    System.out.println("Invalid choice. Try again.");
                    return;
                }

                int tenantIdToDelete = tenantIds.get(choice - 1);
                String sql = "DELETE FROM tenants WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, tenantIdToDelete);
                    pstmt.executeUpdate();
                    System.out.println("Tenant '" + tenantNames.get(choice - 1) + "' deleted successfully.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void increaseRent(String dbName, Scanner scanner) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "")) {
            System.out.print("Enter tenant ID to increase rent: ");
            int id = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter percentage increase (e.g., '10' for 10% increase): ");
            double percentage = Double.parseDouble(scanner.nextLine()) / 100.0;

            String selectSql = "SELECT rent FROM tenants WHERE id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, id);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    double currentRent = rs.getDouble("rent");
                    double newRent = currentRent * (1 + percentage);

                    String updateSql = "UPDATE tenants SET rent = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, newRent);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();
                        System.out.printf("Rent updated from $%.2f to $%.2f\n", currentRent, newRent);
                    }
                } else {
                    System.out.println("Tenant ID not found.");
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Invalid input. Please try again.");
        }
    }

    private static void editTenant(String dbName, Scanner scanner) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "")) {
            System.out.println("\nEditing Tenant Information");

            // List tenants and let user select one
            List<Integer> tenantIds = new ArrayList<>();
            List<String> tenantNames = new ArrayList<>();

            String selectAllSql = "SELECT id, name, apt_number FROM tenants";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectAllSql)) {

                int index = 1;
                while (rs.next()) {
                    tenantIds.add(rs.getInt("id"));
                    tenantNames.add(rs.getString("name") + " (Apt#: " + rs.getString("apt_number") + ")");
                    System.out.println(index + ". " + tenantNames.get(index - 1));
                    index++;
                }

                if (tenantIds.isEmpty()) {
                    System.out.println("No tenants found.");
                    return;
                }

                System.out.print("\nEnter the number of the tenant to edit (or type 'back' to cancel): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("back")) return;

                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice. Please enter a valid number.");
                    return;
                }

                if (choice < 1 || choice > tenantIds.size()) {
                    System.out.println("Invalid choice. Try again.");
                    return;
                }

                int tenantId = tenantIds.get(choice - 1);

                // Editing Menu
                boolean editing = true;
                while (editing) {
                    System.out.println("\nEditing: " + tenantNames.get(choice - 1));
                    System.out.println("1. Edit Name");
                    System.out.println("2. Edit Apartment Number");
                    System.out.println("3. Edit Lease Start Date (MM-DD-YYYY)");
                    System.out.println("4. Edit Lease Expiry Date (MM-DD-YYYY)");
                    System.out.println("5. Edit Security Deposit");
                    System.out.println("6. Edit Monthly Rent");
                    System.out.println("7. Done Editing");

                    System.out.print("Select an option: ");
                    String editChoice = scanner.nextLine();

                    switch (editChoice) {
                        case "1":
                            updateTenantField(conn, tenantId, "name", "Enter new name: ", scanner);
                            break;
                        case "2":
                            updateTenantField(conn, tenantId, "apt_number", "Enter new apartment number: ", scanner);
                            break;
                        case "3":
                            updateTenantField(conn, tenantId, "lease_start", "Enter new lease start date (MM-DD-YYYY): ", scanner, true);
                            break;
                        case "4":
                            updateTenantField(conn, tenantId, "lease_expired", "Enter new lease expiry date (MM-DD-YYYY): ", scanner, true);
                            break;
                        case "5":
                            updateTenantField(conn, tenantId, "security", "Enter new security deposit: ", scanner);
                            break;
                        case "6":
                            updateTenantField(conn, tenantId, "rent", "Enter new monthly rent: ", scanner);
                            break;
                        case "7":
                            editing = false;
                            System.out.println("Finished editing tenant information.");
                            break;
                        default:
                            System.out.println("Invalid option. Try again.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to edit tenant.");
            e.printStackTrace();
        }
    }



    private static void updateTenantField(Connection conn, int tenantId, String field, String prompt, Scanner scanner) {
        updateTenantField(conn, tenantId, field, prompt, scanner, false);
    }

    private static void updateTenantField(Connection conn, int tenantId, String field, String prompt, Scanner scanner, boolean isDate) {
        try {
            System.out.print(prompt);
            String newValue = scanner.nextLine();
            if (newValue.equalsIgnoreCase("back")) return;

            if (isDate) {
                newValue = convertDateFormat(newValue);
            }

            String sql = "UPDATE tenants SET " + field + " = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (field.equals("security") || field.equals("rent")) {
                    pstmt.setDouble(1, Double.parseDouble(newValue));
                } else {
                    pstmt.setString(1, newValue);
                }
                pstmt.setInt(2, tenantId);
                pstmt.executeUpdate();
                System.out.println("Updated successfully.");
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Invalid input. Update failed.");
        }
    }
    private static String convertDateFormat(String date) {
        try {
            String[] parts = date.split("-");
            if (parts.length != 3) throw new IllegalArgumentException();
            return parts[2] + "-" + parts[0] + "-" + parts[1]; // Converts MM-DD-YYYY to YYYY-MM-DD
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use MM-DD-YYYY.");
        }
    }



    private static int getUnpaidMonths(String leaseStart) {
        try {
            String[] parts = leaseStart.split("-");
            int leaseYear = Integer.parseInt(parts[0]);
            int leaseMonth = Integer.parseInt(parts[1]);

            java.time.LocalDate today = java.time.LocalDate.now();
            int currentYear = today.getYear();
            int currentMonth = today.getMonthValue();

            int totalMonths = (currentYear - leaseYear) * 12 + (currentMonth - leaseMonth);
            return Math.max(totalMonths, 0);
        } catch (Exception e) {
            return 0;
        }
    }





}





