package com.dispatchdesk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight built-in REST API backend server powered by JDK 21 HttpServer.
 * Exposes JSON endpoints connected directly to DispatchBackend & SQLite (dispatchdesk.db)
 * with per-user data isolation via userId / X-User-Id.
 */
public class BackendApiServer {
    private static HttpServer server;
    private static volatile boolean running = false;
    private static volatile int activePort = -1;

    public static synchronized boolean start(int port) {
        if (running) return true;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

            server.createContext("/api/health", BackendApiServer::handleHealth);
            server.createContext("/api/auth/login", BackendApiServer::handleLogin);
            server.createContext("/api/auth/register", BackendApiServer::handleRegister);
            server.createContext("/api/dashboard", BackendApiServer::handleDashboard);
            server.createContext("/api/drivers", BackendApiServer::handleDrivers);
            server.createContext("/api/orders", BackendApiServer::handleOrders);
            server.createContext("/api/assignments", BackendApiServer::handleAssignments);
            server.createContext("/api/reports", BackendApiServer::handleReports);

            // Use daemon threads so closing the Swing UI cleanly terminates the JVM
            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "DispatchDesk-BackendAPI");
                t.setDaemon(true);
                return t;
            }));

            server.start();
            running = true;
            activePort = port;
            System.out.println("Backend API Server started at http://127.0.0.1:" + port + "/api/health");
            return true;
        } catch (IOException e) {
            System.err.println("Backend API Server could not bind port " + port + " (" + e.getMessage() + "). Desktop backend remains connected directly to SQLite.");
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
        DispatchBackend.UserSession session = DispatchBackend.login(body.get("username"), body.get("password"));
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
        if (DispatchBackend.usernameExists(username)) {
            sendJson(ex, 409, "{\"error\":\"Username already exists\"}");
            return;
        }
        DispatchBackend.UserSession session = DispatchBackend.register(username, password, fullName, role);
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
            DispatchBackend.DashboardStats s = DispatchBackend.getDashboardStats(userId);
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
                List<DispatchBackend.DriverRecord> drivers = DispatchBackend.getDrivers(userId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < drivers.size(); i++) {
                    DispatchBackend.DriverRecord d = drivers.get(i);
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
                int id = DispatchBackend.addDriver(userId, body.getOrDefault("name", ""), body.getOrDefault("phone", ""), body.getOrDefault("area", ""));
                sendJson(ex, 201, "{\"driverId\":" + id + ",\"userId\":" + userId + "}");
            } else {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
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
                List<DispatchBackend.OrderRecord> orders = DispatchBackend.getOrders(userId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < orders.size(); i++) {
                    DispatchBackend.OrderRecord o = orders.get(i);
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
                int id = DispatchBackend.createOrder(
                        userId,
                        body.getOrDefault("customer", ""),
                        body.getOrDefault("address", ""),
                        body.getOrDefault("area", ""),
                        body.getOrDefault("deliveryDate", ""),
                        body.getOrDefault("timeSlot", "9 AM - 12 PM")
                );
                sendJson(ex, 201, "{\"orderId\":" + id + ",\"userId\":" + userId + "}");
            } else {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleAssignments(HttpExchange ex) throws IOException {
        if (handleCorsPreflight(ex)) return;
        int userId = resolveUserId(ex, null);
        if (userId <= 0) {
            sendJson(ex, 400, "{\"error\":\"Missing or invalid userId\"}");
            return;
        }
        try {
            List<DispatchBackend.AssignmentRecord> list = DispatchBackend.getAssignments(userId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                DispatchBackend.AssignmentRecord a = list.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"assignmentId\":").append(a.assignmentId).append(",")
                        .append("\"orderId\":").append(a.orderId).append(",")
                        .append("\"userId\":").append(a.userId).append(",")
                        .append("\"customer\":\"").append(escapeJson(a.customer)).append("\",")
                        .append("\"driver\":\"").append(escapeJson(a.driver)).append("\",")
                        .append("\"area\":\"").append(escapeJson(a.area)).append("\",")
                        .append("\"status\":\"").append(escapeJson(a.status)).append("\",")
                        .append("\"assignedAt\":\"").append(escapeJson(a.assignedAt)).append("\"")
                        .append("}");
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString());
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
            DispatchBackend.ReportSummary r = DispatchBackend.getReportSummary(userId);
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"userId\":").append(userId).append(",");
            sb.append("\"totalOrders\":").append(r.totalOrders).append(",");
            sb.append("\"deliveredOrders\":").append(r.deliveredOrders).append(",");
            sb.append("\"failedOrders\":").append(r.failedOrders).append(",");
            sb.append("\"successRate\":").append(String.format(java.util.Locale.US, "%.1f", r.successRate)).append(",");
            sb.append("\"areas\":[");
            for (int i = 0; i < r.areaStats.size(); i++) {
                ReportsAnalyticsChart.AreaStat a = r.areaStats.get(i);
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

    // ====================================================
    // HTTP & JSON UTILITIES
    // ====================================================

    private static boolean handleCorsPreflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-User-Id");
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

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        // Matches "key": "value" or "key": 123
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\"|([0-9]+))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2).replace("\\\"", "\"") : matcher.group(3);
            map.put(key, val);
        }
        return map;
    }

    private static String userSessionToJson(DispatchBackend.UserSession s) {
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
