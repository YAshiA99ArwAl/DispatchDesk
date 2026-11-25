package com.dispatchdesk.config;

import java.io.File;
import java.sql.*;

/**
 * Core SQLite Database connection manager and automatic schema migration layer.
 * Connects DispatchDesk's backend services to the persistent dispatchdesk.db SQLite database
 * with multi-user isolation via user_id foreign keys.
 */
public class Database {
    private static volatile String resolvedUrl;

    public static String getDatabaseUrl() {
        if (resolvedUrl != null) {
            return resolvedUrl;
        }
        String override = System.getProperty("dispatchdesk.db");
        if (override == null || override.isBlank()) {
            override = System.getenv("DISPATCHDESK_DB");
        }
        if (override != null && !override.isBlank()) {
            resolvedUrl = "jdbc:sqlite:" + override.trim();
            return resolvedUrl;
        }

        File localDb = new File("dispatchdesk.db");
        File backendDb = new File("backend", "dispatchdesk.db");
        File nestedDb = new File("DispatchDesk", "dispatchdesk.db");
        File backendDir = new File("backend");

        if (localDb.exists()) {
            resolvedUrl = "jdbc:sqlite:" + localDb.getAbsolutePath();
        } else if (backendDb.exists() || backendDir.isDirectory()) {
            resolvedUrl = "jdbc:sqlite:" + backendDb.getAbsolutePath();
        } else if (nestedDb.exists()) {
            resolvedUrl = "jdbc:sqlite:" + nestedDb.getAbsolutePath();
        } else {
            resolvedUrl = "jdbc:sqlite:" + localDb.getAbsolutePath();
        }
        return resolvedUrl;
    }

    public static Connection connect() throws SQLException {
        Connection con = DriverManager.getConnection(getDatabaseUrl());

        try (Statement stmt = con.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 5000");
        }

        return con;
    }

    public static void initialize() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                full_name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'Coordinator'
            )
            """;

        String drivers = """
            CREATE TABLE IF NOT EXISTS drivers (
                driver_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL DEFAULT 1,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                area TEXT NOT NULL,
                available INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
            """;

        String orders = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL DEFAULT 1,
                customer_name TEXT NOT NULL,
                address TEXT NOT NULL,
                area TEXT NOT NULL,
                delivery_date TEXT NOT NULL,
                time_slot TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'Pending',
                failure_reason TEXT,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
            """;

        String assignments = """
            CREATE TABLE IF NOT EXISTS assignments (
                assignment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL DEFAULT 1,
                order_id INTEGER NOT NULL UNIQUE,
                driver_id INTEGER NOT NULL,
                assigned_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (order_id) REFERENCES orders(order_id),
                FOREIGN KEY (driver_id) REFERENCES drivers(driver_id)
            )
            """;

        try (Connection con = connect();
             Statement stmt = con.createStatement()) {

            // 1. Ensure tables exist
            stmt.execute(users);
            stmt.execute(drivers);
            stmt.execute(orders);
            stmt.execute(assignments);

            // 2. Seed default admin coordinator if not present
            String seedAdmin = """
                INSERT OR IGNORE INTO users (username, password, full_name, role)
                VALUES ('admin', 'admin123', 'Dispatch Admin', 'Lead Coordinator')
                """;
            stmt.execute(seedAdmin);

            // 3. Resolve admin's actual user_id for migrating legacy rows safely
            int adminUserId = resolveAdminId(con);

            // 4. Migrate pre-existing databases to add per-user ownership (user_id)
            ensureUserIdColumn(con, "drivers", adminUserId);
            ensureUserIdColumn(con, "orders", adminUserId);
            ensureUserIdColumn(con, "assignments", adminUserId);

            // 5. Create indexes for fast user-scoped backend queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_drivers_user ON drivers(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_assignments_user ON assignments(user_id)");

            System.out.println("Database initialized & connected: " + getDatabaseUrl());

        } catch (SQLException e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }

    private static int resolveAdminId(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT user_id FROM users WHERE LOWER(username) = 'admin' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        }
        return 1;
    }

    private static void ensureUserIdColumn(Connection con, String tableName, int defaultUserId) throws SQLException {
        boolean hasUserId = false;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if ("user_id".equalsIgnoreCase(rs.getString("name"))) {
                    hasUserId = true;
                    break;
                }
            }
        }
        if (!hasUserId) {
            try (Statement st = con.createStatement()) {
                st.execute("ALTER TABLE " + tableName + " ADD COLUMN user_id INTEGER NOT NULL DEFAULT " + defaultUserId);
            }
            System.out.println("Migrated table '" + tableName + "' with per-user column user_id (default owner=" + defaultUserId + ")");
        }
    }

    public static boolean validateUser(String username, String password) {
        if (username == null || password == null) return false;
        String u = username.trim();
        String p = password.trim();
        if (u.isEmpty() || p.isEmpty()) return false;

        String sql = "SELECT password FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, u);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return p.equals(rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return false;
    }

    public static int getUserId(String username) {
        if (username == null || username.trim().isEmpty()) return -1;
        String sql = "SELECT user_id FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("User ID lookup error: " + e.getMessage());
        }
        return -1;
    }

    public static String getUserFullName(String username) {
        if (username == null || username.trim().isEmpty()) return "Dispatch Admin";

        String sql = "SELECT full_name FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("full_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("User lookup error: " + e.getMessage());
        }
        return username.trim();
    }

    public static String getUserRole(String username) {
        if (username == null || username.trim().isEmpty()) return "Coordinator";

        String sql = "SELECT role FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            System.err.println("User role lookup error: " + e.getMessage());
        }
        return "Coordinator";
    }

    public static boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String u = username.trim();

        String sql = "SELECT user_id FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, u);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("User check error: " + e.getMessage());
        }
        return false;
    }

    public static boolean createUser(String username, String password, String fullName, String role) {
        if (username == null || password == null || fullName == null) return false;
        String u = username.trim();
        String p = password.trim();
        String name = fullName.trim();
        String r = (role == null || role.trim().isEmpty()) ? "Coordinator" : role.trim();

        if (u.isEmpty() || p.isEmpty() || name.isEmpty()) return false;
        if (userExists(u)) return false;

        String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection con = connect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, u);
            pstmt.setString(2, p);
            pstmt.setString(3, name);
            pstmt.setString(4, r);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("User creation error: " + e.getMessage());
        }
        return false;
    }
}
