// Tenant.java
package org.core;

import java.time.LocalDate;

public class Tenant {
    private int id;
    private String name;
    private String aptNumber;
    private LocalDate leaseStart;
    private LocalDate leaseExpired;
    private double security;
    private double rent;
    private double balance;

    public Tenant(int id, String name, String aptNumber, LocalDate leaseStart, LocalDate leaseExpired, double security, double rent, double balance) {
        this.id = id;
        this.name = name;
        this.aptNumber = aptNumber;
        this.leaseStart = leaseStart;
        this.leaseExpired = leaseExpired;
        this.security = security;
        this.rent = rent;
        this.balance = balance;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAptNumber() {
        return aptNumber;
    }

    public void setAptNumber(String aptNumber) {
        this.aptNumber = aptNumber;
    }

    public LocalDate getLeaseStart() {
        return leaseStart;
    }

    public void setLeaseStart(LocalDate leaseStart) {
        this.leaseStart = leaseStart;
    }

    public LocalDate getLeaseExpired() {
        return leaseExpired;
    }

    public void setLeaseExpired(LocalDate leaseExpired) {
        this.leaseExpired = leaseExpired;
    }

    public double getSecurity() {
        return security;
    }

    public void setSecurity(double security) {
        this.security = security;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return name + " (Apt " + aptNumber + ") - Balance: $" + balance;
    }
}
