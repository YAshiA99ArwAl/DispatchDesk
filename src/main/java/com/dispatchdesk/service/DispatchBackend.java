package com.dispatchdesk.service;

import com.dispatchdesk.db.Database;
import com.dispatchdesk.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Backend Service & Data Access Layer for DispatchDesk.
 * Connects the application frontend and REST API directly to the SQLite database
 * with strict per-user data isolation (every query is scoped by user_id).
 */
public class DispatchBackend {

    // ====================================================
    // HEALTH CHECK
    // ====================================================

    public static boolean isDatabaseConnected() {
        try (Connection con = Database.connect();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ====================================================
    // AUTHENTICATION & USER ACCOUNT BACKEND
    // ====================================================

    public static UserSession login(String username, String password) {
        if (username == null || password == null) return null;
        String u = username.trim();
        String p = password.trim();
        if (u.isEmpty() || p.isEmpty()) return null;

        String sql = "SELECT user_id, username, password, full_name, role FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && p.equals(rs.getString("password"))) {
                    return new UserSession(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Backend login error: " + e.getMessage());
        }
        return null;
    }

    public static boolean usernameExists(String username) {
        return Database.userExists(username);
    }

    public static UserSession register(String username, String password, String fullName, String role) {
        if (username == null || password == null || fullName == null) return null;
        String u = username.trim();
        String p = password.trim();
        String name = fullName.trim();
        String r = (role == null || role.trim().isEmpty()) ? "Coordinator" : role.trim();

        if (u.isEmpty() || p.isEmpty() || name.isEmpty()) return null;
        if (usernameExists(u)) return null;

        String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u);
            ps.setString(2, p);
            ps.setString(3, name);
            ps.setString(4, r);

            int inserted = ps.executeUpdate();
            if (inserted > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new UserSession(keys.getInt(1), u, name, r);
                    }
                }
                int id = Database.getUserId(u);
                if (id > 0) {
                    return new UserSession(id, u, name, r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Backend register error: " + e.getMessage());
        }
        return null;
    }

    // ====================================================
    // DRIVERS BACKEND (SCOPED BY user_id)
    // ====================================================

    public static List<DriverRecord> getDrivers(int userId) throws SQLException {
        List<DriverRecord> list = new ArrayList<>();
        String sql = """
            SELECT driver_id, user_id, name, phone, area, available
            FROM drivers
            WHERE user_id = ?
            ORDER BY driver_id DESC
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DriverRecord rec = new DriverRecord();
                    rec.driverId = rs.getInt("driver_id");
                    rec.userId = rs.getInt("user_id");
                    rec.name = rs.getString("name");
                    rec.phone = rs.getString("phone");
                    rec.area = rs.getString("area");
                    rec.available = rs.getInt("available") == 1;
                    list.add(rec);
                }
            }
        }
        return list;
    }

    public static int addDriver(int userId, String name, String phone, String area) throws SQLException {
        String sql = "INSERT INTO drivers (user_id, name, phone, area) VALUES (?, ?, ?, ?)";
        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name.trim());
            ps.setString(3, phone.trim());
            ps.setString(4, area.trim());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public static boolean updateDriverAvailability(int userId, int driverId, boolean available) throws SQLException {
        String sql = "UPDATE drivers SET available = ? WHERE driver_id = ? AND user_id = ?";
        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, driverId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ====================================================
    // ORDERS BACKEND (SCOPED BY user_id)
    // ====================================================

    public static List<OrderRecord> getOrders(int userId) throws SQLException {
        List<OrderRecord> list = new ArrayList<>();
        String sql = """
            SELECT order_id, user_id, customer_name, address, area,
                   delivery_date, time_slot, status, failure_reason
            FROM orders
            WHERE user_id = ?
            ORDER BY order_id DESC
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderRecord rec = new OrderRecord();
                    rec.orderId = rs.getInt("order_id");
                    rec.userId = rs.getInt("user_id");
                    rec.customer = rs.getString("customer_name");
                    rec.address = rs.getString("address");
                    rec.area = rs.getString("area");
                    rec.deliveryDate = rs.getString("delivery_date");
                    rec.timeSlot = rs.getString("time_slot");
                    rec.status = rs.getString("status");
                    rec.failureReason = rs.getString("failure_reason");
                    list.add(rec);
                }
            }
        }
        return list;
    }

    public static int createOrder(
            int userId,
            String customer,
            String address,
            String area,
            String deliveryDate,
            String timeSlot) throws SQLException {

        String sql = """
            INSERT INTO orders
            (user_id, customer_name, address, area, delivery_date, time_slot)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, customer.trim());
            ps.setString(3, address.trim());
            ps.setString(4, area.trim());
            ps.setString(5, deliveryDate.trim());
            ps.setString(6, timeSlot.trim());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public static boolean updatePendingOrder(
            int userId,
            int orderId,
            String customer,
            String address,
            String area,
            String deliveryDate,
            String timeSlot) throws SQLException {

        String sql = """
            UPDATE orders
            SET customer_name = ?,
                address = ?,
                area = ?,
                delivery_date = ?,
                time_slot = ?
            WHERE order_id = ?
              AND user_id = ?
              AND status = 'Pending'
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customer.trim());
            ps.setString(2, address.trim());
            ps.setString(3, area.trim());
            ps.setString(4, deliveryDate.trim());
            ps.setString(5, timeSlot.trim());
            ps.setInt(6, orderId);
            ps.setInt(7, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ====================================================
    // ASSIGNMENTS & SMART DISPATCH BACKEND (SCOPED BY user_id)
    // ====================================================

    public static AssignmentResult smartAssignOrder(int userId, int orderId, String area) throws SQLException {
        String checkOrderSql = """
            SELECT status, area FROM orders
            WHERE order_id = ? AND user_id = ?
            """;

        String driverQuery = """
            SELECT d.driver_id,
                   d.name,
                   COUNT(o.order_id) AS workload
            FROM drivers d
            LEFT JOIN assignments a
                ON d.driver_id = a.driver_id
                AND a.user_id = d.user_id
            LEFT JOIN orders o
                ON a.order_id = o.order_id
                AND o.user_id = d.user_id
                AND o.status IN ('Assigned', 'In Progress')
            WHERE d.user_id = ?
              AND LOWER(TRIM(d.area)) = LOWER(TRIM(?))
              AND d.available = 1
            GROUP BY d.driver_id, d.name
            ORDER BY workload ASC, d.driver_id ASC
            LIMIT 1
            """;

        try (Connection con = Database.connect()) {
            con.setAutoCommit(false);
            try {
                // Verify order ownership and Pending status
                String effectiveArea = area;
                try (PreparedStatement ps = con.prepareStatement(checkOrderSql)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new AssignmentResult(false, -1, null, "Order not found in your workspace.");
                        }
                        if (!"Pending".equals(rs.getString("status"))) {
                            con.rollback();
                            return new AssignmentResult(false, -1, null, "Only pending orders can be assigned.");
                        }
                        if (effectiveArea == null || effectiveArea.isBlank()) {
                            effectiveArea = rs.getString("area");
                        }
                    }
                }

                // Find best available driver owned by this user in the target area
                int driverId;
                String driverName;
                try (PreparedStatement ps = con.prepareStatement(driverQuery)) {
                    ps.setInt(1, userId);
                    ps.setString(2, effectiveArea);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new AssignmentResult(
                                    false, -1, null,
                                    "No available driver found in " + effectiveArea + " for your account."
                            );
                        }
                        driverId = rs.getInt("driver_id");
                        driverName = rs.getString("name");
                    }
                }

                // Insert assignment scoped to user_id
                String insertSql = "INSERT INTO assignments (user_id, order_id, driver_id) VALUES (?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, orderId);
                    ps.setInt(3, driverId);
                    ps.executeUpdate();
                }

                // Update order status scoped to user_id
                String updateSql = "UPDATE orders SET status = 'Assigned' WHERE order_id = ? AND user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                con.commit();
                return new AssignmentResult(true, driverId, driverName, "Driver assigned successfully!");

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public static List<AssignmentRecord> getAssignments(int userId) throws SQLException {
        List<AssignmentRecord> list = new ArrayList<>();
        String sql = """
            SELECT a.assignment_id,
                   o.order_id,
                   a.user_id,
                   o.customer_name,
                   d.name AS driver_name,
                   o.area,
                   o.status,
                   a.assigned_at,
                   o.failure_reason
            FROM assignments a
            JOIN orders o ON a.order_id = o.order_id
            JOIN drivers d ON a.driver_id = d.driver_id
            WHERE a.user_id = ?
              AND o.user_id = ?
              AND d.user_id = ?
            ORDER BY a.assignment_id DESC
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AssignmentRecord rec = new AssignmentRecord();
                    rec.assignmentId = rs.getInt("assignment_id");
                    rec.orderId = rs.getInt("order_id");
                    rec.userId = rs.getInt("user_id");
                    rec.customer = rs.getString("customer_name");
                    rec.driver = rs.getString("driver_name");
                    rec.area = rs.getString("area");
                    rec.status = rs.getString("status");
                    rec.assignedAt = rs.getString("assigned_at");
                    rec.failureReason = rs.getString("failure_reason");
                    list.add(rec);
                }
            }
        }
        return list;
    }

    public static boolean updateDeliveryStatus(
            int userId,
            int orderId,
            String newStatus,
            String failureReason) throws SQLException {

        String sql = """
            UPDATE orders
            SET status = ?, failure_reason = ?
            WHERE order_id = ? AND user_id = ?
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, failureReason);
            ps.setInt(3, orderId);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ====================================================
    // DASHBOARD & REPORTS ANALYTICS BACKEND (SCOPED BY user_id)
    // ====================================================

    public static DashboardStats getDashboardStats(int userId) throws SQLException {
        DashboardStats stats = new DashboardStats();

        String orderStatsSql = """
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END), 0) AS pending,
                COALESCE(SUM(CASE WHEN status = 'Delivered' THEN 1 ELSE 0 END), 0) AS delivered,
                COALESCE(SUM(CASE WHEN status = 'Failed' THEN 1 ELSE 0 END), 0) AS failed,
                COALESCE(SUM(CASE WHEN status = 'Assigned' THEN 1 ELSE 0 END), 0) AS assigned,
                COALESCE(SUM(CASE WHEN status = 'In Progress' THEN 1 ELSE 0 END), 0) AS in_progress
            FROM orders
            WHERE user_id = ?
            """;

        String driverStatsSql = """
            SELECT
                COUNT(*) AS total_drivers,
                COALESCE(SUM(CASE WHEN available = 1 THEN 1 ELSE 0 END), 0) AS available_drivers
            FROM drivers
            WHERE user_id = ?
            """;

        try (Connection con = Database.connect()) {
            try (PreparedStatement ps = con.prepareStatement(orderStatsSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.totalOrders = rs.getInt("total");
                        stats.pendingOrders = rs.getInt("pending");
                        stats.deliveredOrders = rs.getInt("delivered");
                        stats.failedOrders = rs.getInt("failed");
                        stats.assignedOrders = rs.getInt("assigned");
                        stats.inProgressOrders = rs.getInt("in_progress");
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(driverStatsSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.totalDrivers = rs.getInt("total_drivers");
                        stats.availableDrivers = rs.getInt("available_drivers");
                    }
                }
            }
        }

        return stats;
    }

    public static ReportSummary getReportSummary(int userId) throws SQLException {
        ReportSummary summary = new ReportSummary();

        String summarySql = """
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN status = 'Delivered' THEN 1 ELSE 0 END), 0) AS delivered,
                COALESCE(SUM(CASE WHEN status = 'Failed' THEN 1 ELSE 0 END), 0) AS failed
            FROM orders
            WHERE user_id = ?
            """;

        String areaSql = """
            SELECT
                area,
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN status = 'Delivered' THEN 1 ELSE 0 END), 0) AS delivered,
                COALESCE(SUM(CASE WHEN status = 'Failed' THEN 1 ELSE 0 END), 0) AS failed
            FROM orders
            WHERE user_id = ?
            GROUP BY area
            ORDER BY area
            """;

        try (Connection con = Database.connect()) {
            try (PreparedStatement ps = con.prepareStatement(summarySql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.totalOrders = rs.getInt("total");
                        summary.deliveredOrders = rs.getInt("delivered");
                        summary.failedOrders = rs.getInt("failed");
                        summary.successRate = summary.totalOrders == 0
                                ? 0.0
                                : (summary.deliveredOrders * 100.0 / summary.totalOrders);
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(areaSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        summary.areaStats.add(new AreaStat(
                                rs.getString("area"),
                                rs.getInt("total"),
                                rs.getInt("delivered"),
                                rs.getInt("failed")
                        ));
                    }
                }
            }
        }

        return summary;
    }
}
