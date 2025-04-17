package org.core;

import java.sql.*;


public class DatabaseManager {
    public static void initializeDatabase(String dbName) {
        String DB_URL = "jdbc:h2:./" + dbName;

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS tenants ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(255), "
                    + "apt_number VARCHAR(50), "
                    + "lease_start DATE, "
                    + "lease_expired DATE, "
                    + "security DOUBLE, "
                    + "rent DOUBLE, "
                    + "jan_paid DOUBLE DEFAULT 0, "
                    + "feb_paid DOUBLE DEFAULT 0, "
                    + "mar_paid DOUBLE DEFAULT 0, "
                    + "apr_paid DOUBLE DEFAULT 0, "
                    + "may_paid DOUBLE DEFAULT 0, "
                    + "jun_paid DOUBLE DEFAULT 0, "
                    + "jul_paid DOUBLE DEFAULT 0, "
                    + "aug_paid DOUBLE DEFAULT 0, "
                    + "sep_paid DOUBLE DEFAULT 0, "
                    + "oct_paid DOUBLE DEFAULT 0, "
                    + "nov_paid DOUBLE DEFAULT 0, "
                    + "dec_paid DOUBLE DEFAULT 0, "
                    + "balance DOUBLE DEFAULT 0)";
            stmt.execute(sql);
            System.out.println("Database initialized: " + dbName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

