package com.dispatchdesk.model;

/** Represents an order-to-driver dispatch assignment record. */
public class AssignmentRecord {
    public int assignmentId;
    public int orderId;
    public int userId;
    public String customer;
    public String driver;
    public String area;
    public String status;
    public String assignedAt;
    public String failureReason;
}
