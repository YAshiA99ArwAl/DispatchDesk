package com.dispatchdesk.api;

import com.dispatchdesk.model.*;
import com.dispatchdesk.service.DispatchBackend;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone lightweight REST API backend server powered by JDK 21 HttpServer.
 * Exposes JSON endpoints connected directly to DispatchBackend & SQLite (dispatchdesk.db)
 * with per-user data isolation via userId / X-User-Id.
 * Also serves static frontend assets so the full system can be run locally with zero extra tools.
 */
public class BackendApiServer {
    private static HttpServer server;
    private static volatile boolean running = false;
    private static volatile int activePort = -1;

    public static synchronized boolean start(int port) {
        if (running) return true;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/api/health", BackendApiServer::handleHealth);
            server.createContext("/api/auth/login", BackendApiServer::handleLogin);
            server.createContext("/api/auth/register", BackendApiServer::handleRegister);
            server.createContext("/api/dashboard", BackendApiServer::handleDashboard);
            server.createContext("/api/drivers/availability", BackendApiServer::handleDriverAvailability);
            server.createContext("/api/drivers", BackendApiServer::handleDrivers);
            server.createContext("/api/orders/assign", BackendApiServer::handleOrderAssign);
            server.createContext("/api/orders", BackendApiServer::handleOrders);
            server.createContext("/api/assignments/status", BackendApiServer::handleAssignmentStatus);
            server.createContext("/api/assignments", BackendApiServer::handleAssignments);
            server.createContext("/api/reports", BackendApiServer::handleReports);
            server.createContext("/", BackendApiServer::handleStatic);

            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "DispatchDesk-BackendAPI");
                t.setDaemon(false);
                return t;
            }));

            server.start();
            running = true;
            activePort = port;
            System.out.println("Backend API Server started at http://localhost:" + port + "/api/health");
            return true;
        } catch (IOException e) {
            System.err.println("Backend API Server could not bind port " + port + ": " + e.getMessage());
            running = false;
            activePort = -1;
            return false;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        running = false;
        activePort = -1;
    }

    public static boolean isRunning() {
        return running;
    }

    public static int getActivePort() {
        return activePort;
    }

    // ====================================================
    // ROUTE HANDLERS
    // ====================================================

    private static void handleHealth(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        boolean dbOk = DispatchBackend.isDatabaseConnected();
        String json = "{"
                + "\"status\":\"" + (dbOk ? "ok" : "degraded") + "\","
                + "\"database\":\"" + (dbOk ? "connected" : "disconnected") + "\","
                + "\"port\":" + activePort
                + "}";
        sendJson(ex, dbOk ? 200 : 503, json);
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseSimpleJson(readBody(ex));
        UserSession session = DispatchBackend.login(body.get("username"), body.get("password"));
        if (session == null) {
            sendJson(ex, 401, "{\"error\":\"Invalid username or password\"}");
            return;
        }
        sendJson(ex, 200, userSessionToJson(session));
    }

    private static void handleRegister(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseSimpleJson(readBody(ex));
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String role = body.getOrDefault("role", "Coordinator");

        if (username == null || password == null || fullName == null
                || username.isBlank() || password.isBlank() || fullName.isBlank()) {
            sendJson(ex, 400, "{\"error\":\"username, password, and fullName are required\"}");
            return;
        }
        if (username.trim().length() < 3) {
            sendJson(ex, 400, "{\"error\":\"Username/ID must be at least 3 characters\"}");
            return;
        }
        if (password.trim().length() < 4) {
            sendJson(ex, 400, "{\"error\":\"Password must be at least 4 characters\"}");
            return;
        }
        if (DispatchBackend.usernameExists(username)) {
            sendJson(ex, 409, "{\"error\":\"Username already exists\"}");
            return;
        }
        UserSession session = DispatchBackend.register(username, password, fullName, role);
        if (session == null) {
            sendJson(ex, 500, "{\"error\":\"Could not register user\"}");
            return;
        }
        sendJson(ex, 201, userSessionToJson(session));
    }

    private static void handleDashboard(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        int userId = resolveUserId(ex, null);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        try {
            DashboardStats s = DispatchBackend.getDashboardStats(userId);
            String json = "{"
                    + "\"userId\":" + userId + ","
                    + "\"totalOrders\":" + s.totalOrders + ","
                    + "\"pendingOrders\":" + s.pendingOrders + ","
                    + "\"assignedOrders\":" + s.assignedOrders + ","
                    + "\"inProgressOrders\":" + s.inProgressOrders + ","
                    + "\"deliveredOrders\":" + s.deliveredOrders + ","
                    + "\"failedOrders\":" + s.failedOrders + ","
                    + "\"totalDrivers\":" + s.totalDrivers + ","
                    + "\"availableDrivers\":" + s.availableDrivers
                    + "}";
            sendJson(ex, 200, json);
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleDrivers(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        String method = ex.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                int userId = resolveUserId(ex, null);
                if (userId <= 0) {
                    sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
                    return;
                }
                List<DriverRecord> drivers = DispatchBackend.getDrivers(userId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < drivers.size(); i++) {
                    DriverRecord d = drivers.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                            .append("\"driverId\":").append(d.driverId).append(",")
                            .append("\"userId\":").append(d.userId).append(",")
                            .append("\"name\":\"").append(escapeJson(d.name)).append("\",")
                            .append("\"phone\":\"").append(escapeJson(d.phone)).append("\",")
                            .append("\"area\":\"").append(escapeJson(d.area)).append("\",")
                            .append("\"available\":").append(d.available)
                            .append("}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> body = parseSimpleJson(readBody(ex));
                int userId = resolveUserId(ex, body);
                if (userId <= 0) {
                    sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
                    return;
                }
                String name = body.getOrDefault("name", "").trim();
                String phone = body.getOrDefault("phone", "").trim();
                String area = body.getOrDefault("area", "").trim();
                if (name.isEmpty() || phone.isEmpty() || area.isEmpty()) {
                    sendJson(ex, 400, "{\"error\":\"name, phone, and area are required\"}");
                    return;
                }
                int id = DispatchBackend.addDriver(userId, name, phone, area);
                sendJson(ex, 201, "{\"driverId\":" + id + ",\"userId\":" + userId + "}");
            } else {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleDriverAvailability(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        String method = ex.getRequestMethod();
        if (!"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseSimpleJson(readBody(ex));
        int userId = resolveUserId(ex, body);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        int driverId = parseIntOrDefault(body.get("driverId"), -1);
        if (driverId <= 0 || !body.containsKey("available")) {
            sendJson(ex, 400, "{\"error\":\"driverId and available are required\"}");
            return;
        }
        boolean available = Boolean.parseBoolean(body.get("available"));
        try {
            boolean updated = DispatchBackend.updateDriverAvailability(userId, driverId, available);
            if (!updated) {
                sendJson(ex, 404, "{\"error\":\"Driver not found in your workspace\"}");
                return;
            }
            sendJson(ex, 200, "{\"updated\":true,\"driverId\":" + driverId + ",\"available\":" + available + "}");
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleOrders(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        String method = ex.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                int userId = resolveUserId(ex, null);
                if (userId <= 0) {
                    sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
                    return;
                }
                List<OrderRecord> orders = DispatchBackend.getOrders(userId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < orders.size(); i++) {
                    OrderRecord o = orders.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                            .append("\"orderId\":").append(o.orderId).append(",")
                            .append("\"userId\":").append(o.userId).append(",")
                            .append("\"customer\":\"").append(escapeJson(o.customer)).append("\",")
                            .append("\"address\":\"").append(escapeJson(o.address)).append("\",")
                            .append("\"area\":\"").append(escapeJson(o.area)).append("\",")
                            .append("\"deliveryDate\":\"").append(escapeJson(o.deliveryDate)).append("\",")
                            .append("\"timeSlot\":\"").append(escapeJson(o.timeSlot)).append("\",")
                            .append("\"status\":\"").append(escapeJson(o.status)).append("\",")
                            .append("\"failureReason\":").append(o.failureReason == null ? "null" : "\"" + escapeJson(o.failureReason) + "\"")
                            .append("}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> body = parseSimpleJson(readBody(ex));
                int userId = resolveUserId(ex, body);
                if (userId <= 0) {
                    sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
                    return;
                }
                String customer = body.getOrDefault("customer", "").trim();
                String address = body.getOrDefault("address", "").trim();
                String area = body.getOrDefault("area", "").trim();
                String deliveryDate = body.getOrDefault("deliveryDate", "").trim();
                String timeSlot = body.getOrDefault("timeSlot", "9 AM - 12 PM").trim();

                if (customer.isEmpty() || address.isEmpty() || area.isEmpty() || deliveryDate.isEmpty()) {
                    sendJson(ex, 400, "{\"error\":\"customer, address, area, and deliveryDate are required\"}");
                    return;
                }
                try {
                    LocalDate.parse(deliveryDate);
                } catch (DateTimeParseException dtpe) {
                    sendJson(ex, 400, "{\"error\":\"Enter a valid deliveryDate in YYYY-MM-DD format\"}");
                    return;
                }

                int id = DispatchBackend.createOrder(userId, customer, address, area, deliveryDate, timeSlot);
                sendJson(ex, 201, "{\"orderId\":" + id + ",\"userId\":" + userId + "}");
            } else if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                Map<String, String> body = parseSimpleJson(readBody(ex));
                int userId = resolveUserId(ex, body);
                if (userId <= 0) {
                    sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
                    return;
                }
                int orderId = parseIntOrDefault(body.get("orderId"), -1);
                String customer = body.getOrDefault("customer", "").trim();
                String address = body.getOrDefault("address", "").trim();
                String area = body.getOrDefault("area", "").trim();
                String deliveryDate = body.getOrDefault("deliveryDate", "").trim();
                String timeSlot = body.getOrDefault("timeSlot", "").trim();

                if (orderId <= 0 || customer.isEmpty() || address.isEmpty() || area.isEmpty()
                        || deliveryDate.isEmpty() || timeSlot.isEmpty()) {
                    sendJson(ex, 400, "{\"error\":\"orderId, customer, address, area, deliveryDate, and timeSlot are required\"}");
                    return;
                }
                try {
                    LocalDate.parse(deliveryDate);
                } catch (DateTimeParseException dtpe) {
                    sendJson(ex, 400, "{\"error\":\"Enter a valid deliveryDate in YYYY-MM-DD format\"}");
                    return;
                }

                boolean updated = DispatchBackend.updatePendingOrder(
                        userId, orderId, customer, address, area, deliveryDate, timeSlot
                );
                if (!updated) {
                    sendJson(ex, 409, "{\"error\":\"Only pending orders in your workspace can be updated\"}");
                    return;
                }
                sendJson(ex, 200, "{\"updated\":true,\"orderId\":" + orderId + "}");
            } else {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleOrderAssign(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseSimpleJson(readBody(ex));
        int userId = resolveUserId(ex, body);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        int orderId = parseIntOrDefault(body.get("orderId"), -1);
        if (orderId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Valid orderId is required\"}");
            return;
        }
        String area = body.get("area");
        try {
            AssignmentResult result = DispatchBackend.smartAssignOrder(userId, orderId, area);
            if (!result.success) {
                sendJson(ex, 409, "{\"success\":false,\"error\":\"" + escapeJson(result.message) + "\"}");
                return;
            }
            String json = "{"
                    + "\"success\":true,"
                    + "\"orderId\":" + orderId + ","
                    + "\"driverId\":" + result.driverId + ","
                    + "\"driverName\":\"" + escapeJson(result.driverName) + "\","
                    + "\"message\":\"" + escapeJson(result.message) + "\""
                    + "}";
            sendJson(ex, 200, json);
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleAssignments(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        int userId = resolveUserId(ex, null);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        try {
            List<AssignmentRecord> list = DispatchBackend.getAssignments(userId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                AssignmentRecord a = list.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"assignmentId\":").append(a.assignmentId).append(",")
                        .append("\"orderId\":").append(a.orderId).append(",")
                        .append("\"userId\":").append(a.userId).append(",")
                        .append("\"customer\":\"").append(escapeJson(a.customer)).append("\",")
                        .append("\"driver\":\"").append(escapeJson(a.driver)).append("\",")
                        .append("\"area\":\"").append(escapeJson(a.area)).append("\",")
                        .append("\"status\":\"").append(escapeJson(a.status)).append("\",")
                        .append("\"assignedAt\":\"").append(escapeJson(a.assignedAt)).append("\",")
                        .append("\"failureReason\":").append(a.failureReason == null ? "null" : "\"" + escapeJson(a.failureReason) + "\"")
                        .append("}");
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleAssignmentStatus(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        String method = ex.getRequestMethod();
        if (!"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseSimpleJson(readBody(ex));
        int userId = resolveUserId(ex, body);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        int orderId = parseIntOrDefault(body.get("orderId"), -1);
        String status = body.getOrDefault("status", "").trim();
        String failureReason = body.get("failureReason");
        if (failureReason != null) {
            failureReason = failureReason.trim();
            if ("null".equalsIgnoreCase(failureReason) || failureReason.isEmpty()) {
                failureReason = null;
            }
        }

        if (orderId <= 0 || status.isEmpty()) {
            sendJson(ex, 400, "{\"error\":\"orderId and status are required\"}");
            return;
        }
        if (!"In Progress".equals(status) && !"Delivered".equals(status) && !"Failed".equals(status)) {
            sendJson(ex, 400, "{\"error\":\"Status must be 'In Progress', 'Delivered', or 'Failed'\"}");
            return;
        }
        if ("Failed".equals(status) && (failureReason == null || failureReason.isBlank())) {
            sendJson(ex, 400, "{\"error\":\"Failure reason cannot be empty when marking status as Failed\"}");
            return;
        }

        try {
            boolean updated = DispatchBackend.updateDeliveryStatus(userId, orderId, status, failureReason);
            if (!updated) {
                sendJson(ex, 404, "{\"error\":\"Order not found in your workspace\"}");
                return;
            }
            sendJson(ex, 200, "{\"updated\":true,\"orderId\":" + orderId + ",\"status\":\"" + escapeJson(status) + "\"}");
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleReports(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        int userId = resolveUserId(ex, null);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        try {
            ReportSummary r = DispatchBackend.getReportSummary(userId);
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"userId\":").append(userId).append(",");
            sb.append("\"totalOrders\":").append(r.totalOrders).append(",");
            sb.append("\"deliveredOrders\":").append(r.deliveredOrders).append(",");
            sb.append("\"failedOrders\":").append(r.failedOrders).append(",");
            sb.append("\"successRate\":").append(String.format(java.util.Locale.US, "%.1f", r.successRate)).append(",");
            sb.append("\"areas\":[");
            for (int i = 0; i < r.areaStats.size(); i++) {
                AreaStat a = r.areaStats.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"area\":\"").append(escapeJson(a.area)).append("\",")
                        .append("\"total\":").append(a.total).append(",")
                        .append("\"delivered\":").append(a.delivered).append(",")
                        .append("\"failed\":").append(a.failed)
                        .append("}");
            }
            sb.append("]}");
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod()) && !"HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String path = ex.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        // Locate frontend directory (check current dir, parent dir, or user.dir)
        File frontendDir = null;
        File[] candidates = {
            new File("frontend"),
            new File("../frontend"),
            new File(new File(System.getProperty("user.dir")), "frontend"),
            new File(new File(System.getProperty("user.dir")).getParentFile(), "frontend")
        };
        for (File candidate : candidates) {
            if (candidate.isDirectory()) {
                frontendDir = candidate.getCanonicalFile();
                break;
            }
        }

        if (frontendDir == null) {
            String msg = "Frontend directory not found on server.";
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            ex.sendResponseHeaders(404, msg.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(msg.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        // Prevent path traversal
        File targetFile = new File(frontendDir, path.startsWith("/") ? path.substring(1) : path).getCanonicalFile();
        if (!targetFile.getPath().startsWith(frontendDir.getPath()) || !targetFile.exists() || targetFile.isDirectory()) {
            File indexFile = new File(frontendDir, "index.html");
            if (indexFile.exists() && !path.startsWith("/api/")) {
                targetFile = indexFile;
            } else {
                String notFound = "404 Not Found";
                ex.sendResponseHeaders(404, notFound.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
        }

        String mime = getMimeType(targetFile.getName());
        byte[] bytes = Files.readAllBytes(targetFile.toPath());
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    // ====================================================
    // HTTP & JSON UTILITIES
    // ====================================================

    private static boolean handleCorsPreflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-User-Id, Authorization");
        ex.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int resolveUserId(HttpExchange ex, Map<String, String> body) {
        String headerVal = ex.getRequestHeaders().getFirst("X-User-Id");
        if (headerVal != null && !headerVal.isBlank()) {
            try { return Integer.parseInt(headerVal.trim()); } catch (NumberFormatException ignored) {}
        }
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    try {
                        return Integer.parseInt(URLDecoder.decode(kv[1], StandardCharsets.UTF_8).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (body != null && body.containsKey("userId")) {
            try { return Integer.parseInt(body.get("userId").trim()); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private static int parseIntOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\"|(true|false|null|-?[0-9]+))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null
                    ? matcher.group(2).replace("\\\"", "\"").replace("\\\\", "\\")
                    : matcher.group(3);
            if (!"null".equals(val) || matcher.group(2) != null) {
                map.put(key, val);
            }
        }
        return map;
    }

    private static String userSessionToJson(UserSession s) {
        return "{"
                + "\"userId\":" + s.userId + ","
                + "\"username\":\"" + escapeJson(s.username) + "\","
                + "\"fullName\":\"" + escapeJson(s.fullName) + "\","
                + "\"role\":\"" + escapeJson(s.role) + "\""
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
