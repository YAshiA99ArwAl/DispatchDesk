package com.dispatchdesk.model;

/**
 * Result payload returned by the smart-assign dispatch workflow.
 */
public class AssignmentResult {
    public final boolean success;
    public final int driverId;
    public final String driverName;
    public final String message;

    public AssignmentResult(boolean success, int driverId, String driverName, String message) {
        this.success = success;
        this.driverId = driverId;
        this.driverName = driverName;
        this.message = message;
    }
}
