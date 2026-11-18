package com.dispatchdesk.model;

/**
 * Represents a customer delivery order owned by a coordinator workspace.
 */
public class OrderRecord {
    public int orderId;
    public int userId;
    public String customer;
    public String address;
    public String area;
    public String deliveryDate;
    public String timeSlot;
    public String status;
    public String failureReason;
}
