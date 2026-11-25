package com.dispatchdesk.model;

/** Aggregated dashboard KPI counts for orders and fleet drivers. */
public class DashboardStats {
    public int totalOrders;
    public int pendingOrders;
    public int deliveredOrders;
    public int failedOrders;
    public int assignedOrders;
    public int inProgressOrders;
    public int totalDrivers;
    public int availableDrivers;
}
