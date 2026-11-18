package com.dispatchdesk.model;

/**
 * Represents a driver-to-order dispatch assignment record.
 */
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
