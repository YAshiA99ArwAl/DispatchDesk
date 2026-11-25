# 🚚 DispatchDesk — Modern SaaS Delivery & Fleet Management System

DispatchDesk is a high-performance **decoupled client-server delivery and fleet management system**. It features a **strictly Java 21 REST backend** with SQLite persistence and an **interactive, modern SaaS web frontend** ready for 1-click deployment on **Vercel**.

---

## 🎨 Design System & Palette

The user interface follows a modern SaaS executive design system:

| Hex Code | Color Name | Role in UI |
| :--- | :--- | :--- |
| **`#F4EEFF`** | Soft Lavender | Canvas, backgrounds, and subtle card borders |
| **`#DCD6F7`** | Lilac Accent | Active navigation pills, table dividers, accent borders |
| **`#A6B1E1`** | Slate Periwinkle | Interactive highlights, focus rings, secondary actions |
| **`#424874`** | Deep Navy | Brand identity, primary buttons, high-contrast headings |

---

## 🏗️ Project Architecture

```text
DispatchDesk/
├── run.bat                           # Root one-click launcher (Windows)
├── vercel.json                       # Root Vercel deployment configuration
├── README.md                         # Complete documentation & Viva Q&A
├── .gitignore                        # Git exclusion rules
│
├── backend/                          # Strictly Java 21 + SQLite REST API
│   ├── pom.xml                       # Maven build descriptor
│   ├── run-backend.bat               # Windows launcher (auto-detects JDK)
│   ├── run-backend.sh                # Linux / macOS launcher
│   ├── Dockerfile                    # Container deployment (Render / Railway)
│   ├── dispatchdesk.db               # SQLite database file
│   ├── lib/
│   │   └── sqlite-jdbc-3.50.3.0.jar  # Bundled driver for zero-prerequisite running
│   └── src/main/java/com/dispatchdesk/
│       ├── BackendMain.java          # Standalone server entry point
│       ├── config/Database.java      # SQLite connection & schema migrations
│       ├── model/                    # Domain DTOs (Order, Driver, Assignment, Stats)
│       ├── service/DispatchBackend.java # Business logic, transactions & user isolation
│       └── api/BackendApiServer.java # Built-in JDK HttpServer JSON REST API & static server
│
└── frontend/                         # Static Web UI (Vercel-Ready)
    ├── vercel.json                   # Standalone Vercel config & security headers
    ├── index.html                    # Single-Page SaaS Application
    ├── css/styles.css                # Custom modern SaaS styling
    └── js/
        ├── config.js                 # Dynamic Backend API URL management
        ├── api.js                    # Fetch REST client with offline preview fallback
        ├── charts.js                 # SVG Area Bar Chart, Pipeline & Ring Gauge
        └── app.js                    # Auth, Orders, Drivers, Assignments & Reports
```

---

## ✨ Features & Capabilities

- **🔐 Authentication & Coordinator Registration ("Create ID")**:
  - Secure login with username/password.
  - Interactive show/hide password toggle.
  - **Create ID** registration tab with instant duplicate-check validation.
  - Multi-user isolation (`user_id` scoping across all tables).
  - Default administrative account seeded (`admin` / `admin123`).

- **📊 Executive Dashboard**:
  - 4 Key Summary KPI cards: Total Orders, Pending Action, In Transit, Delivered.
  - Dynamic visual pipeline progress bar with status proportions.
  - Real-time fleet driver readiness indicator.

- **📦 Orders Management**:
  - SaaS table with customer avatars, address, area, date, time slot, and status badges.
  - Create new orders with validation.
  - Edit pending orders directly from modal dialogs.
  - One-click **Smart Assign** button that automatically pairs the order with the optimal available driver.

- **🚚 Drivers Directory**:
  - Driver roster showing assigned delivery zone, contact phone, and availability status.
  - Real-time driver availability toggle (`Available` / `Busy`).
  - Add new drivers dynamically.

- **🔄 Delivery Assignments**:
  - Live dispatch mapping linking orders to assigned drivers.
  - Instant delivery status lifecycle updates: `Assigned` → `In Progress` → `Delivered` / `Failed`.
  - Mandatory failure reason recording for audit trails.

- **📈 Reports & Fleet Analytics**:
  - SVG Area & Bar Chart visualizing delivery volume and completion metrics.
  - Circular fulfillment donut ring gauge indicating fleet efficiency.
  - Zone-wise breakdown table with per-area success rates.

---

## 🚀 Running Locally

### Option 1: One-Click Launch (Windows)
Double-click `run.bat` in the project root:
```cmd
run.bat
```
This compiles and starts the Java backend. Once running:
- **Backend API**: `http://localhost:8080/api/health`
- **Web UI**: `http://localhost:8080/` (served directly by Java with zero external tools)

### Option 2: Using Maven
```bash
cd backend
mvn compile exec:java
```

### Option 3: Running the Frontend Standalone
You can also serve `frontend/` using any static server (or VS Code Live Server):
```bash
npx serve frontend
```

---

## ☁️ Deploying the Frontend on Vercel

1. Push this repository to GitHub.
2. In **Vercel** ([vercel.com](https://vercel.com)):
   - Click **Add New → Project**.
   - Import your GitHub repository (`DispatchDesk`).
   - Leave the settings as default (the root `vercel.json` automatically configures `"outputDirectory": "frontend"`).
   - Click **Deploy**.
3. **Connecting Vercel Frontend to Java Backend**:
   - Run the Java backend locally (`http://localhost:8080`) on the demo machine, or deploy `backend/Dockerfile` on Render/Railway/Fly.io.
   - For local demos with Vercel, run `ngrok http 8080` or `localtunnel`.
   - Click the **API URL** button in the top bar of the deployed site and paste the backend URL (or pass `?api=https://your-backend-url` in the browser URL).
   - *Note: If the backend is not running, the frontend automatically offers an **Offline UI Preview Mode** so all screens and UI features can still be viewed.*

---

## 🔌 REST API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Backend and SQLite database health check |
| `POST` | `/api/auth/login` | Authenticate coordinator credentials |
| `POST` | `/api/auth/register` | Register new coordinator account |
| `GET` | `/api/dashboard` | Dashboard KPIs and fleet summary statistics |
| `GET` / `POST` | `/api/orders` | List orders for current user / create new order |
| `PUT` | `/api/orders` | Update a pending order |
| `POST` | `/api/orders/assign` | Smart-assign driver to pending order |
| `GET` / `POST` | `/api/drivers` | List drivers / add driver |
| `PUT` | `/api/drivers/availability` | Toggle driver availability |
| `GET` | `/api/assignments` | List active and past delivery assignments |
| `PUT` | `/api/assignments/status` | Update delivery status (`In Progress`, `Delivered`, `Failed`) |
| `GET` | `/api/reports` | Fleet fulfillment metrics and area-wise analytics |

---

## 🎓 Viva Voce Comprehensive Q&A (For Professor's Questions)

### Q1: Why did you decouple the frontend and backend into separate layers?
> **Answer**: Decoupling adheres to the **Separation of Concerns (SoC)** architectural principle. The backend strictly handles data validation, SQL transactions, persistence, and dispatch business rules. The frontend focuses entirely on user experience, responsiveness, and state presentation. Decoupling allows the frontend to be statically hosted on a global CDN (like Vercel) with zero server latency, while the backend can run independently as a secure REST API.

### Q2: Why did you use standard JDK `HttpServer` instead of Spring Boot?
> **Answer**: Spring Boot introduces heavy dependencies, auto-configuration magic, and high memory overhead (~300MB+ RAM for simple CRUD). Using Java's built-in `com.sun.net.httpserver.HttpServer` keeps the application lightweight, startup time sub-second, footprint under 30MB RAM, and 100% transparent. Every HTTP exchange, routing mechanism, JSON serialization, and SQL query is explicitly written and easy to explain.

### Q3: How does the Smart Driver Assignment algorithm (`smartAssignOrder`) work?
> **Answer**: When assigning a pending order:
> 1. It verifies the order exists in `Pending` status and belongs to the authenticated coordinator.
> 2. It queries available drivers (`available = 1`) assigned to the same geographical delivery area (`LOWER(TRIM(d.area)) = LOWER(TRIM(?))`).
> 3. It orders eligible drivers by **active workload** (`COUNT(order_id)` of currently `Assigned` or `In Progress` orders) ascending, breaking ties by `driver_id ASC`.
> 4. The driver with the lowest workload is selected to balance delivery load evenly across the fleet.

### Q4: How do you prevent race conditions or inconsistent states during order assignment?
> **Answer**: We use **ACID transactions** in SQLite via JDBC:
> ```java
> con.setAutoCommit(false);
> // 1. INSERT INTO assignments ...
> // 2. UPDATE orders SET status = 'Assigned' ...
> con.commit();
> ```
> If any exception occurs between the assignment record creation and the order status update, `con.rollback()` is executed. This guarantees atomicity—an order can never be assigned without an assignment record, and vice versa.

### Q5: How is multi-user data isolation achieved?
> **Answer**: Every primary entity table (`drivers`, `orders`, `assignments`) includes a `user_id INTEGER NOT NULL REFERENCES users(user_id)`. The frontend passes the coordinator's authenticated user ID via the `X-User-Id` HTTP header. Every backend service method includes `WHERE user_id = ?` in its SQL statements and indexes (`idx_orders_user`, `idx_drivers_user`). Coordinators can never see or modify each other's data.

### Q6: What is CORS and why is CORS preflight (`OPTIONS`) handled?
> **Answer**: **Cross-Origin Resource Sharing (CORS)** is a browser security mechanism that restricts web applications on one origin (e.g., `https://dispatchdesk.vercel.app`) from querying an API on another origin (e.g., `http://localhost:8080`). When custom headers like `X-User-Id` or `Content-Type: application/json` are sent, the browser issues an `OPTIONS` preflight request. Our `handleCorsPreflight()` method responds with HTTP 204 and headers (`Access-Control-Allow-Origin: *`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`) so the browser permits the subsequent REST API calls.

### Q7: How does SQLite handle concurrency in a multi-threaded web server?
> **Answer**: SQLite supports multiple simultaneous readers but serializes write transactions using table/file-level locking. To prevent `SQLITE_BUSY` lock contention under concurrent HTTP threads, we execute `PRAGMA busy_timeout = 5000` on every connection. If another thread is writing, SQLite automatically retries for up to 5 seconds before returning a busy exception.

---

## 🔑 Default Credentials

- **Username**: `admin`
- **Password**: `admin123`

*(You can also click **"Create ID"** on the login page to register a new coordinator account with its own isolated fleet and orders.)*

---

## 📄 License
MIT License. Created for Java academic laboratory project and dispatch fleet operations.
