package com.dispatchdesk.model;

/** Represents a customer delivery order record scoped to a coordinator account. */
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
