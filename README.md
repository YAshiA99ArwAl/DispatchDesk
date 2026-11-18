# 🚚 DispatchDesk - Modern SaaS Dispatch & Fleet Management System

DispatchDesk is a modern, high-performance desktop dispatch management platform engineered in Java Swing with custom Java2D vector graphics and SQLite persistence. It provides real-time operational oversight for delivery coordinators, fleet dispatchers, and logistics managers.

---

## 🎨 Design System & Palette

The user interface follows a modern SaaS design language utilizing a pastel color palette:

| Hex Code | Color Name | Role |
| :--- | :--- | :--- |
| **`#F4EEFF`** | Soft Lavender | Canvas, backgrounds, and light card borders |
| **`#DCD6F7`** | Lilac Accent | Active navigation pills, table dividers, accent borders |
| **`#A6B1E1`** | Slate Periwinkle | Interactive highlights, focus rings, secondary actions |
| **`#424874`** | Deep Navy | Brand identity, primary buttons, high-contrast headings |

---

## ✨ Features & Capabilities

- **🔐 Authentication, "Create ID" & Per-User Data Isolation**:
  - Secure login screen with username/email and password fields.
  - Interactive Show/Hide password toggle with custom-rendered pill controls.
  - Built-in **Create ID** registration tab allowing coordinators to create an account with full credential validation and duplicate ID checking.
  - **Strict Multi-User Data Isolation**: Every coordinator account has its own private workspace scoped by `user_id` in SQLite (`orders`, `drivers`, `assignments`, dashboard metrics, and reports are isolated per user).
  - Session management with user profile display and immediate logout capability (clears in-memory session state on sign-out).

- **🧩 Backend Service & Built-In REST API**:
  - **`service/DispatchBackend.java`**: Dedicated Java backend service & repository layer connecting all UI operations cleanly to SQLite with parameterized, user-scoped queries and atomic SQL transactions.
  - **`api/BackendApiServer.java`**: Built-in lightweight HTTP REST API (`http://127.0.0.1:8080/api/*`) exposing `/api/health`, `/api/auth/login`, `/api/auth/register`, `/api/dashboard`, `/api/drivers`, `/api/orders`, `/api/assignments`, and `/api/reports`.

- **📊 Operational Dashboard**:
  - 4 key summary metrics: Total Orders, Pending Action, In Transit, Delivered.
  - Vertically expanded **Delivery Pipeline & Fleet Overview** with multi-segmented visual progress tracking.
  - Driver allocation gauges and real-time system readiness chips with antialiased Java2D vector checkmarks.
  - Anchored **Refresh Dashboard** action bar for real-time fleet synchronization.

- **📦 Orders Management**:
  - SaaS table layout with generous 48px row heights, subtle hairline separators, and soft hover effects.
  - Customer avatar monogram badges with initial generation.
  - Color-coded status pills (`Delivered`, `In Progress`, `Pending`, `Failed`).
  - Interactive **View Details →** popup dialog for comprehensive order inspection.

- **🚚 Drivers Directory**:
  - Driver management with real-time status tracking (`Available`, `Busy`, `On Route`).
  - Fast status toggling directly from table actions.
  - Dedicated driver profile dialog displaying contact information, vehicle type, and current assignment status.

- **🔄 Delivery Assignments**:
  - Real-time order-to-driver dispatch mapping.
  - Multi-avatar table displaying customer and assigned driver badges.
  - Assignment inspector popup for delivery lifecycle audit.

- **📈 Reports & Fleet Analytics**:
  - Custom Java2D Area & Bar Chart visualizing delivery volume and completion metrics.
  - Circular fulfillment donut ring gauge indicating fleet efficiency.
  - Historical delivery logs and operational summaries.

---

## 🛠️ Technology Stack

- **Language**: Java 21 (LTS)
- **GUI Framework**: Java Swing & Java2D (Custom Antialiased Vector Components)
- **Database**: SQLite 3 (via `sqlite-jdbc 3.50.3.0`)
- **Build System**: Apache Maven

---

## 📁 Project Structure

```text
DispatchDesk/
├── pom.xml
├── README.md
├── run.bat
├── data/
│   └── dispatchdesk.db
└── src/
    ├── main/
    │   ├── java/com/dispatchdesk/
    │   │   ├── Main.java                            # Application bootstrap & entry point
    │   │   ├── api/
    │   │   │   └── BackendApiServer.java            # Embedded JDK HttpServer REST API
    │   │   ├── db/
    │   │   │   └── Database.java                    # SQLite connection & schema migrations
    │   │   ├── model/
    │   │   │   ├── AreaStat.java                    # Area fulfillment statistics DTO
    │   │   │   ├── AssignmentRecord.java            # Assignment record DTO
    │   │   │   ├── AssignmentResult.java            # Smart-assign response DTO
    │   │   │   ├── DashboardStats.java              # Dashboard KPI counts DTO
    │   │   │   ├── DriverRecord.java                # Driver record DTO
    │   │   │   ├── OrderRecord.java                 # Order record DTO
    │   │   │   ├── ReportSummary.java               # Reports summary DTO
    │   │   │   └── UserSession.java                 # Authenticated user session DTO
    │   │   ├── service/
    │   │   │   └── DispatchBackend.java             # Business logic & user-scoped data access
    │   │   └── ui/
    │   │       ├── MainFrame.java                   # Main Swing window & navigation shell
    │   │       ├── theme/
    │   │       │   └── UITheme.java                 # Design tokens, colors & fonts
    │   │       ├── components/
    │   │       │   ├── DashboardAnalyticsPanel.java # Delivery pipeline & fleet overview widget
    │   │       │   ├── DashboardStatCard.java       # Reusable KPI metric card
    │   │       │   ├── ModernComponents.java        # Custom buttons, cards, inputs & tables
    │   │       │   └── ReportsAnalyticsChart.java   # Area bar chart & success ratio gauge
    │   │       ├── dialogs/
    │   │       │   └── DetailsDialogs.java          # Order & assignment inspection dialogs
    │   │       └── panels/
    │   │           ├── AssignmentsPanel.java        # Delivery assignments view
    │   │           ├── DashboardPanel.java          # Operational dashboard view
    │   │           ├── DriversPanel.java            # Drivers directory view
    │   │           ├── LoginPanel.java              # Sign In & Create ID view
    │   │           ├── OrdersPanel.java             # Orders management view
    │   │           └── ReportsPanel.java            # Reports & analytics view
    │   └── resources/
    └── test/
        └── java/com/dispatchdesk/
```

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK) 21** or higher.
- **Apache Maven** (optional, wrapper included).

### Running the Application

#### Option 1: Using the provided Run Script (Windows)
Double-click `run.bat` or execute in PowerShell / Command Prompt:
```cmd
run.bat
```

#### Option 2: Using Maven
```bash
mvn clean compile exec:java -Dexec.mainClass="com.dispatchdesk.Main"
```

#### Option 3: IntelliJ IDEA
1. Open the `DispatchDesk` folder in IntelliJ IDEA.
2. Ensure Project SDK is set to **JDK 21**.
3. Run `Main.java` (`src/main/java/com/dispatchdesk/Main.java`).

---

## 🔑 Default Credentials

- **Username**: `admin`
- **Password**: `admin123`

*Or click **"Create ID"** on the login screen to register your own coordinator account.*

---

## 📄 License
MIT License. Created for dispatch and logistics fleet operations.
