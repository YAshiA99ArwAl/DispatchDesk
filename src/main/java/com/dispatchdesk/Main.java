package com.dispatchdesk;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private TableRowSorter<DefaultTableModel> orderSorter;
    private JTextField orderSearchField;
    private JComboBox<String> orderStatusFilter;
    private JTable orderTable;
    private DefaultTableModel assignmentModel;

    // All original metric labels preserved
    private JLabel totalOrders, pendingOrders,
            deliveredOrders, totalDrivers,
            failedOrders, assignedOrders,
            inProgressOrders, availableDrivers;

    // Reports page components
    private JLabel reportTotalOrders;
    private JLabel reportDeliveredOrders;
    private JLabel reportFailedOrders;
    private JLabel reportSuccessRate;

    private DefaultTableModel reportAreaModel;
    private DefaultTableModel driverModel, orderModel;

    // Modern analytics widget for dashboard
    private DashboardAnalyticsPanel analyticsPanel;

    // Modern chart widget for reports
    private ReportsAnalyticsChart reportsChart;

    // In-memory full record caches for rich details popups
    public static class OrderRecord {
        public int orderId;
        public String customer;
        public String address;
        public String area;
        public String deliveryDate;
        public String timeSlot;
        public String status;
        public String failureReason;
    }
    private final List<OrderRecord> orderRecords = new ArrayList<>();

    public static class AssignmentRecord {
        public int orderId;
        public String customer;
        public String driver;
        public String area;
        public String status;
        public String assignedAt;
        public String failureReason;
    }
    private final List<AssignmentRecord> assignmentRecords = new ArrayList<>();

    // DispatchDesk colour palette: F4EEFF DCD6F7 A6B1E1 424874
    private static final Color SIDEBAR = Color.WHITE;
    private static final Color SIDEBAR_ACTIVE = UITheme.ACCENT_LAVENDER;
    private static final Color BACKGROUND = UITheme.BG_CANVAS;
    private static final Color CARD = UITheme.CARD_BG;
    private static final Color PRIMARY = UITheme.PRIMARY_DARK;
    private static final Color PRIMARY_HOVER = new Color(52, 57, 92);
    private static final Color TEXT = UITheme.TEXT_PRIMARY;
    private static final Color MUTED = UITheme.TEXT_SECONDARY;
    private static final Color BORDER = UITheme.CARD_BORDER;
    private static final Color SUCCESS = UITheme.STATUS_DELIVERED_FG;
    private static final Color DANGER = UITheme.STATUS_FAILED_FG;

    // Authentication & Session State (Per-User Data Isolation)
    private boolean authenticated = false;
    private int currentUserId = -1;
    private String currentUsername = "";
    private String currentFullName = "";
    private CardLayout mainCardLayout;
    private JPanel mainContainer;
    private LoginPanel loginPanel;
    private JPanel appPanel;
    private JLabel userNameLabel;
    private JComponent userAvatarComponent;
    private JLabel dashboardSubtitleLabel;
    private CardLayout pagesCardLayout;
    private JPanel pagesContainer;
    private JLabel pageHeadingLabel;
    private JButton dashboardNavBtn;
    private JButton ordersNavBtn;
    private JButton driversNavBtn;
    private JButton assignmentsNavBtn;
    private JButton reportsNavBtn;

    public Main() {
        applyTheme();
        Database.initialize();
        BackendApiServer.start(8080);

        setTitle("DispatchDesk - Local Delivery Coordinator");
        setSize(1220, 800);
        setMinimumSize(new Dimension(1040, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainCardLayout = new CardLayout();
        mainContainer = new JPanel(mainCardLayout);

        // 1. Modern Login Screen
        loginPanel = new LoginPanel(this::onLoginSuccess);

        // 2. Main Dashboard & Workspace Panel
        appPanel = createAppPanel();

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(appPanel, "APP");

        add(mainContainer);

        // Route Protection: Unauthenticated users start directly on LOGIN page
        mainCardLayout.show(mainContainer, "LOGIN");
        loginPanel.focusInitial();
    }

    private void onLoginSuccess(DispatchBackend.UserSession session) {
        if (session == null || session.userId <= 0) return;
        this.authenticated = true;
        this.currentUserId = session.userId;
        this.currentUsername = session.username;
        this.currentFullName = session.fullName;

        if (userNameLabel != null) {
            userNameLabel.setText(session.fullName);
            userNameLabel.setToolTipText("Signed in as @" + session.username + " (ID #" + session.userId + ")");
        }
        if (userAvatarComponent != null) {
            userAvatarComponent.repaint();
        }
        if (dashboardSubtitleLabel != null) {
            dashboardSubtitleLabel.setText(
                    "Workspace for " + session.fullName + " (@" + session.username + ") · Essential delivery metrics and fleet readiness summary"
            );
        }

        // Reset search/status filters so new user starts with an unfiltered view
        if (orderSearchField != null) {
            orderSearchField.setText("");
        }
        if (orderStatusFilter != null) {
            orderStatusFilter.setSelectedIndex(0);
        }

        // Always land on Dashboard for the newly signed-in user
        if (pagesCardLayout != null && pagesContainer != null) {
            pagesCardLayout.show(pagesContainer, "Dashboard");
        }
        if (pageHeadingLabel != null) {
            pageHeadingLabel.setText("Dashboard");
        }
        if (dashboardNavBtn != null) {
            selectNavButton(dashboardNavBtn, ordersNavBtn, driversNavBtn, assignmentsNavBtn, reportsNavBtn);
        }

        mainCardLayout.show(mainContainer, "APP");
        refreshAll();
    }

    private void clearUserWorkspaceData() {
        orderRecords.clear();
        assignmentRecords.clear();
        if (driverModel != null) driverModel.setRowCount(0);
        if (orderModel != null) orderModel.setRowCount(0);
        if (assignmentModel != null) assignmentModel.setRowCount(0);
        if (reportAreaModel != null) reportAreaModel.setRowCount(0);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of DispatchDesk?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            this.authenticated = false;
            this.currentUserId = -1;
            this.currentUsername = "";
            this.currentFullName = "";
            clearUserWorkspaceData();
            loginPanel.resetFields();
            mainCardLayout.show(mainContainer, "LOGIN");
            loginPanel.focusInitial();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "DD";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return ("" + name.charAt(0)).toUpperCase();
    }

    private JPanel createAppPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        // ====================================================
        // LEFT SIDEBAR (Inspired by Reference Designs 1 & 4)
        // ====================================================
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JPanel sidebarContent = new JPanel();
        sidebarContent.setOpaque(false);
        sidebarContent.setLayout(new BoxLayout(sidebarContent, BoxLayout.Y_AXIS));
        sidebarContent.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        // Brand Logo & Monogram
        JPanel brandPanel = new JPanel(new BorderLayout(12, 0));
        brandPanel.setOpaque(false);
        brandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent logoBadge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(PRIMARY);
                g2.fill(new RoundRectangle2D.Float(0, 0, 36, 36, 10, 10));

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int x = (36 - fm.stringWidth("D")) / 2;
                int y = (36 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("D", x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
        };

        JPanel brandTextPanel = new JPanel();
        brandTextPanel.setOpaque(false);
        brandTextPanel.setLayout(new BoxLayout(brandTextPanel, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("DispatchDesk");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(PRIMARY);

        JLabel subtitle = new JLabel("DISPATCH PORTAL");
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitle.setForeground(MUTED);

        brandTextPanel.add(brand);
        brandTextPanel.add(Box.createVerticalStrut(2));
        brandTextPanel.add(subtitle);

        brandPanel.add(logoBadge, BorderLayout.WEST);
        brandPanel.add(brandTextPanel, BorderLayout.CENTER);

        sidebarContent.add(brandPanel);
        sidebarContent.add(Box.createVerticalStrut(32));

        // Navigation Section Label
        JLabel navLabel = new JLabel("MAIN MENU");
        navLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        navLabel.setForeground(new Color(155, 158, 188));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 0));
        sidebarContent.add(navLabel);

        // ====================================================
        // MAIN WORKSPACE & PAGES
        // ====================================================
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(BACKGROUND);

        // TOP HEADER
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)
        ));

        JLabel heading = new JLabel("Dashboard");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(PRIMARY);
        this.pageHeadingLabel = heading;

        // Header Right: Status badge
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        JLabel statusPill = new JLabel("● Backend Connected") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.STATUS_DELIVERED_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusPill.setForeground(UITheme.STATUS_DELIVERED_FG);
        statusPill.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        statusPill.setOpaque(false);
        statusPill.setToolTipText("SQLite Database + DispatchBackend Connected (API Port 8080)");

        headerRight.add(statusPill);

        header.add(heading, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);

        // CardLayout Pages
        JPanel pages = new JPanel(new CardLayout());
        pages.setBackground(BACKGROUND);
        this.pagesContainer = pages;

        pages.add(createDashboard(), "Dashboard");
        pages.add(createOrdersPanel(), "Orders");
        pages.add(createDriversPanel(), "Drivers");
        pages.add(createAssignmentsPanel(), "Assignments");
        pages.add(createReportsPanel(), "Reports");

        CardLayout layout = (CardLayout) pages.getLayout();
        this.pagesCardLayout = layout;

        // Modern Navigation Buttons with clean Vector Icons
        JButton dashboardBtn   = new ModernComponents.NavButton("Dashboard", ModernComponents.NavIconType.DASHBOARD);
        JButton ordersBtn      = new ModernComponents.NavButton("Orders", ModernComponents.NavIconType.ORDERS);
        JButton driversBtn     = new ModernComponents.NavButton("Drivers", ModernComponents.NavIconType.DRIVERS);
        JButton assignmentsBtn = new ModernComponents.NavButton("Assignments", ModernComponents.NavIconType.ASSIGNMENTS);
        JButton reportsBtn     = new ModernComponents.NavButton("Reports", ModernComponents.NavIconType.REPORTS);
        this.dashboardNavBtn = dashboardBtn;
        this.ordersNavBtn = ordersBtn;
        this.driversNavBtn = driversBtn;
        this.assignmentsNavBtn = assignmentsBtn;
        this.reportsNavBtn = reportsBtn;

        if (dashboardBtn instanceof ModernComponents.NavButton nb) {
            nb.setActive(true);
        }

        dashboardBtn.addActionListener(e -> {
            layout.show(pages, "Dashboard");
            heading.setText("Dashboard");
            header.revalidate();
            header.repaint();
            selectNavButton(dashboardBtn, ordersBtn, driversBtn, assignmentsBtn, reportsBtn);
        });

        ordersBtn.addActionListener(e -> {
            layout.show(pages, "Orders");
            heading.setText("Orders Management");
            header.revalidate();
            header.repaint();
            selectNavButton(ordersBtn, dashboardBtn, driversBtn, assignmentsBtn, reportsBtn);
        });

        driversBtn.addActionListener(e -> {
            layout.show(pages, "Drivers");
            heading.setText("Drivers Directory");
            header.revalidate();
            header.repaint();
            selectNavButton(driversBtn, dashboardBtn, ordersBtn, assignmentsBtn, reportsBtn);
        });

        assignmentsBtn.addActionListener(e -> {
            layout.show(pages, "Assignments");
            heading.setText("Delivery Assignments");
            header.revalidate();
            header.repaint();
            selectNavButton(assignmentsBtn, dashboardBtn, ordersBtn, driversBtn, reportsBtn);
        });

        reportsBtn.addActionListener(e -> {
            layout.show(pages, "Reports");
            heading.setText("Delivery Reports & Analytics");
            header.revalidate();
            header.repaint();
            selectNavButton(reportsBtn, dashboardBtn, ordersBtn, driversBtn, assignmentsBtn);
            loadReportData();
        });

        sidebarContent.add(dashboardBtn);
        sidebarContent.add(Box.createVerticalStrut(6));
        sidebarContent.add(ordersBtn);
        sidebarContent.add(Box.createVerticalStrut(6));
        sidebarContent.add(driversBtn);
        sidebarContent.add(Box.createVerticalStrut(6));
        sidebarContent.add(assignmentsBtn);
        sidebarContent.add(Box.createVerticalStrut(6));
        sidebarContent.add(reportsBtn);

        // Sidebar Footer: User Profile & Logout (Online status completely removed)
        JPanel userFooter = new JPanel(new BorderLayout(8, 0));
        userFooter.setOpaque(false);
        userFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        userAvatarComponent = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.ACCENT_LAVENDER);
                g2.fillOval(0, 0, 32, 32);

                g2.setColor(PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String in = getInitials(currentFullName);
                int x = (32 - fm.stringWidth(in)) / 2;
                int y = (32 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(in, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }
        };

        userNameLabel = new JLabel(currentFullName);
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userNameLabel.setForeground(PRIMARY);

        ModernComponents.ModernButton logoutBtn = new ModernComponents.ModernButton(
                "Logout", ModernComponents.ButtonStyle.OUTLINE
        );
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        logoutBtn.addActionListener(e -> logout());

        userFooter.add(userAvatarComponent, BorderLayout.WEST);
        userFooter.add(userNameLabel, BorderLayout.CENTER);
        userFooter.add(logoutBtn, BorderLayout.EAST);

        sidebar.add(sidebarContent, BorderLayout.NORTH);
        sidebar.add(userFooter, BorderLayout.SOUTH);

        workspace.add(header, BorderLayout.NORTH);
        workspace.add(pages, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(workspace, BorderLayout.CENTER);

        return root;
    }

    // ====================================================
    // DASHBOARD PANEL - 4 MAIN METRICS & SUBTLE PIPELINE
    // ====================================================
    private JPanel createDashboard() {
        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setBackground(BACKGROUND);
        scrollContainer.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header Title within Dashboard
        JPanel dashHeader = new JPanel(new BorderLayout());
        dashHeader.setOpaque(false);
        dashHeader.setPreferredSize(new Dimension(0, 48));
        dashHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel title = new JLabel("Operational Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(PRIMARY);

        JLabel sub = new JLabel("Essential delivery metrics and fleet readiness summary");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(MUTED);
        this.dashboardSubtitleLabel = sub;

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(sub);

        dashHeader.add(titleBlock, BorderLayout.WEST);
        scrollContainer.add(dashHeader);
        scrollContainer.add(Box.createVerticalStrut(20));

        // Main 4 KPI Metric Cards (1x4 spacious, neat layout)
        JPanel cards = new JPanel(new GridLayout(1, 4, 18, 18));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        totalOrders = new JLabel("0");
        pendingOrders = new JLabel("0");
        inProgressOrders = new JLabel("0");
        deliveredOrders = new JLabel("0");

        // Supporting labels preserved for calculations & data feeds
        assignedOrders = new JLabel("0");
        failedOrders = new JLabel("0");
        totalDrivers = new JLabel("0");
        availableDrivers = new JLabel("0");

        // The 4 Primary Cards:
        cards.add(new DashboardStatCard("Total Orders", totalOrders, "📦", UITheme.BG_CANVAS, PRIMARY, "All registered orders"));
        cards.add(new DashboardStatCard("Pending Action", pendingOrders, "⏳", UITheme.STATUS_PENDING_BG, UITheme.STATUS_PENDING_FG, "Awaiting assignment"));
        cards.add(new DashboardStatCard("In Transit", inProgressOrders, "🚚", UITheme.ACCENT_LAVENDER, PRIMARY, "Active deliveries"));
        cards.add(new DashboardStatCard("Delivered", deliveredOrders, "✓", UITheme.STATUS_DELIVERED_BG, UITheme.STATUS_DELIVERED_FG, "Completed fulfillment"));

        scrollContainer.add(cards);
        scrollContainer.add(Box.createVerticalStrut(22));

        // Subtle Palette-matched Pipeline & Fleet Breakdown Card (Expanded Vertically)
        analyticsPanel = new DashboardAnalyticsPanel();
        analyticsPanel.setPreferredSize(new Dimension(0, 390));
        analyticsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 410));
        scrollContainer.add(analyticsPanel);
        scrollContainer.add(Box.createVerticalStrut(18));

        // Full-width Bottom Actions & Status Bar (Anchored at the bottom)
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        bottomBar.setPreferredSize(new Dimension(0, 46));

        JPanel statusInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        statusInfo.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(30, 130, 60)); // Live active green
        dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel statusText = new JLabel("All dispatch services operational · Real-time pipeline active");
        statusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.setForeground(MUTED);
        statusInfo.add(dot);
        statusInfo.add(statusText);

        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh Dashboard", ModernComponents.ButtonStyle.PRIMARY
        );
        refresh.addActionListener(e -> refreshDashboard());

        bottomBar.add(statusInfo, BorderLayout.WEST);
        bottomBar.add(refresh, BorderLayout.EAST);

        scrollContainer.add(bottomBar);

        JScrollPane scrollPane = new JScrollPane(scrollContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createCard(String title, JLabel value) {
        return new DashboardStatCard(
                title, value, "●", UITheme.BG_CANVAS, PRIMARY, "Metric"
        );
    }

    // ====================================================
    // DRIVERS PANEL
    // ====================================================
    private JPanel createDriversPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        ModernComponents.RoundedCard card = new ModernComponents.RoundedCard(16);
        card.setLayout(new BorderLayout(12, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        driverModel = new DefaultTableModel(
                new String[]{
                        "Driver ID", "Driver Name", "Phone Number", "Delivery Area", "Availability", "Quick Action"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(driverModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setCellRenderer(new ModernComponents.StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setCellRenderer(new ModernComponents.ActionLinkRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow >= 0 && (e.getClickCount() == 2 || viewCol == 5)) {
                    toggleDriverAvailability(table);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);

        // Drivers Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel sectionTitle = new JLabel("Active Fleet Drivers");
        sectionTitle.setFont(UITheme.FONT_SECTION);
        sectionTitle.setForeground(PRIMARY);

        JLabel hint = new JLabel("Click any row or 'Toggle Status' to update availability");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(MUTED);

        titleBlock.add(sectionTitle);
        titleBlock.add(hint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        ModernComponents.ModernButton add = new ModernComponents.ModernButton(
                "+ Add Driver", ModernComponents.ButtonStyle.PRIMARY
        );
        ModernComponents.ModernButton availability = new ModernComponents.ModernButton(
                "Change Availability", ModernComponents.ButtonStyle.SECONDARY
        );
        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh", ModernComponents.ButtonStyle.OUTLINE
        );

        add.addActionListener(e -> addDriver());
        refresh.addActionListener(e -> refreshAll());
        availability.addActionListener(e -> toggleDriverAvailability(table));

        buttons.add(add);
        buttons.add(availability);
        buttons.add(refresh);

        toolbar.add(titleBlock, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void toggleDriverAvailability(JTable table) {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a driver first."
            );
            return;
        }

        String rawId = table.getValueAt(selectedRow, 0).toString().replace("#", "").trim();
        int driverId = Integer.parseInt(rawId);

        String currentAvailability =
                table.getValueAt(selectedRow, 4).toString();

        boolean newAvailability =
                !(currentAvailability.equalsIgnoreCase("Yes") || currentAvailability.contains("Available"));

        try {
            boolean updated = DispatchBackend.updateDriverAvailability(currentUserId, driverId, newAvailability);
            if (updated) {
                String message = newAvailability
                        ? "Driver is now Available."
                        : "Driver is now Unavailable.";
                JOptionPane.showMessageDialog(this, message);
                refreshAll();
            } else {
                showError("Driver could not be updated.");
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    // ====================================================
    // ASSIGNMENTS PANEL (With Details Popup)
    // ====================================================
    private JPanel createAssignmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        ModernComponents.RoundedCard card = new ModernComponents.RoundedCard(16);
        card.setLayout(new BorderLayout(12, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Simplified clean columns - no raw spreadsheet clutter
        assignmentModel = new DefaultTableModel(
                new String[]{
                        "Order ID", "Customer", "Assigned Driver",
                        "Delivery Area", "Status", "Quick Action"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(assignmentModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setCellRenderer(new ModernComponents.StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setCellRenderer(new ModernComponents.ActionLinkRenderer());

        // Row Click or Action Click triggers full Details Popup
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (e.getClickCount() == 2 || viewCol == 5) {
                        openAssignmentDetails(modelRow);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);

        // Header Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel sectionTitle = new JLabel("Driver Assignments & Status");
        sectionTitle.setFont(UITheme.FONT_SECTION);
        sectionTitle.setForeground(PRIMARY);

        JLabel hint = new JLabel("Click any row or 'View Details' for dispatch info");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(MUTED);

        titleBlock.add(sectionTitle);
        titleBlock.add(hint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        ModernComponents.ModernButton update = new ModernComponents.ModernButton(
                "Update Delivery Status", ModernComponents.ButtonStyle.PRIMARY
        );
        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh Assignments", ModernComponents.ButtonStyle.OUTLINE
        );

        refresh.addActionListener(e -> loadAssignments());
        update.addActionListener(e -> updateDeliveryStatus(table));

        buttons.add(update);
        buttons.add(refresh);

        toolbar.add(titleBlock, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void openAssignmentDetails(int modelRow) {
        if (modelRow < 0 || modelRow >= assignmentRecords.size()) return;
        AssignmentRecord rec = assignmentRecords.get(modelRow);
        DetailsDialogs.showAssignmentDetails(
                this,
                rec.orderId,
                rec.customer,
                rec.driver,
                rec.area,
                rec.status,
                rec.assignedAt,
                rec.failureReason,
                () -> updateSingleAssignmentStatus(rec.orderId, rec.status)
        );
    }

    private void updateSingleAssignmentStatus(int orderId, String currentStatus) {
        if (currentStatus.equals("Delivered") || currentStatus.equals("Failed")) {
            JOptionPane.showMessageDialog(this, "This delivery is already completed.");
            return;
        }

        String[] statuses = {"In Progress", "Delivered", "Failed"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        ModernComponents.styleComboBox(statusBox);

        JPanel panel = new JPanel(new GridLayout(2, 1, 6, 8));
        panel.add(new JLabel("Select updated delivery status:"));
        panel.add(statusBox);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Update Status - Order #" + orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return;

        String newStatus = statusBox.getSelectedItem().toString();
        String failureReason = null;

        if (newStatus.equals("Failed")) {
            failureReason = JOptionPane.showInputDialog(
                    this, "Please enter the reason for delivery failure:",
                    "Delivery Failure Reason", JOptionPane.QUESTION_MESSAGE
            );
            if (failureReason == null) return;
            failureReason = failureReason.trim();
            if (failureReason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Failure reason cannot be empty.");
                return;
            }
        }

        try {
            boolean updated = DispatchBackend.updateDeliveryStatus(currentUserId, orderId, newStatus, failureReason);
            if (updated) {
                JOptionPane.showMessageDialog(this, "Delivery status updated to " + newStatus);
                refreshAll();
            } else {
                showError("Order could not be updated.");
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    // ====================================================
    // REPORTS PANEL (With Graphical Chart + Ring Gauge)
    // ====================================================
    private JPanel createReportsPanel() {
        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setBackground(BACKGROUND);
        scrollContainer.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Statistics Cards (Reference Design 2)
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 16));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        reportTotalOrders = new JLabel("0");
        reportDeliveredOrders = new JLabel("0");
        reportFailedOrders = new JLabel("0");
        reportSuccessRate = new JLabel("0%");

        cards.add(new DashboardStatCard("Total Tracked", reportTotalOrders, "📦", UITheme.BG_CANVAS, PRIMARY, "All-time volume"));
        cards.add(new DashboardStatCard("Delivered", reportDeliveredOrders, "✓", UITheme.STATUS_DELIVERED_BG, UITheme.STATUS_DELIVERED_FG, "Fulfilled orders"));
        cards.add(new DashboardStatCard("Failed", reportFailedOrders, "✕", UITheme.STATUS_FAILED_BG, UITheme.STATUS_FAILED_FG, "Delivery issues"));
        cards.add(new DashboardStatCard("Success Rate", reportSuccessRate, "📈", UITheme.ACCENT_LAVENDER, PRIMARY, "Fulfillment ratio"));

        scrollContainer.add(cards);
        scrollContainer.add(Box.createVerticalStrut(18));

        // Beautiful Graphical Chart Widget (Area Comparison Bar Chart + Donut Ring Gauge)
        reportsChart = new ReportsAnalyticsChart();
        reportsChart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        scrollContainer.add(reportsChart);
        scrollContainer.add(Box.createVerticalStrut(18));

        // Area-wise performance table card
        ModernComponents.RoundedCard tableCard = new ModernComponents.RoundedCard(16);
        tableCard.setLayout(new BorderLayout(12, 14));
        tableCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        reportAreaModel = new DefaultTableModel(
                new String[]{
                        "Delivery Area", "Total Orders", "Delivered",
                        "Failed", "Success Rate"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable areaTable = new JTable(reportAreaModel);
        styleTable(areaTable);

        JScrollPane scroll = new JScrollPane(areaTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("Area-wise Performance Breakdown");
        tableTitle.setFont(UITheme.FONT_SECTION);
        tableTitle.setForeground(PRIMARY);

        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh Reports", ModernComponents.ButtonStyle.PRIMARY
        );
        refresh.addActionListener(e -> loadReportData());

        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(refresh, BorderLayout.EAST);

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        scrollContainer.add(tableCard);

        JScrollPane mainScroll = new JScrollPane(scrollContainer);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setBackground(BACKGROUND);
        mainScroll.getViewport().setBackground(BACKGROUND);
        mainScroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        mainScroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND);
        wrapper.add(mainScroll, BorderLayout.CENTER);

        return wrapper;
    }

    private void loadAssignments() {
        assignmentRecords.clear();
        assignmentModel.setRowCount(0);
        if (!authenticated || currentUserId <= 0) return;

        try {
            List<DispatchBackend.AssignmentRecord> rows = DispatchBackend.getAssignments(currentUserId);
            for (DispatchBackend.AssignmentRecord item : rows) {
                AssignmentRecord rec = new AssignmentRecord();
                rec.orderId = item.orderId;
                rec.customer = item.customer;
                rec.driver = item.driver;
                rec.area = item.area;
                rec.status = item.status;
                rec.assignedAt = item.assignedAt;
                rec.failureReason = item.failureReason;
                assignmentRecords.add(rec);

                assignmentModel.addRow(new Object[]{
                        "#" + rec.orderId,
                        rec.customer,
                        rec.driver,
                        rec.area,
                        rec.status,
                        "View Details →"
                });
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateDeliveryStatus(JTable table) {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this, "Please select an assignment first."
            );
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow >= 0 && modelRow < assignmentRecords.size()) {
            AssignmentRecord rec = assignmentRecords.get(modelRow);
            updateSingleAssignmentStatus(rec.orderId, rec.status);
        }
    }

    private void addDriver() {
        JTextField name = new ModernComponents.ModernTextField("e.g. John Doe", 15);
        JTextField phone = new ModernComponents.ModernTextField("e.g. +1 555-0199", 15);
        JTextField area = new ModernComponents.ModernTextField("e.g. Downtown", 15);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        form.setOpaque(false);
        form.add(new JLabel("Driver Name:"));
        form.add(name);
        form.add(new JLabel("Phone:"));
        form.add(phone);
        form.add(new JLabel("Delivery Area:"));
        form.add(area);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Add New Fleet Driver", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().isBlank() ||
                phone.getText().isBlank() ||
                area.getText().isBlank()) {
            showError("Please fill all fields.");
            return;
        }

        try {
            DispatchBackend.addDriver(
                    currentUserId,
                    name.getText().trim(),
                    phone.getText().trim(),
                    area.getText().trim()
            );

            JOptionPane.showMessageDialog(this, "Driver added successfully!");
            refreshAll();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadDrivers() {
        driverModel.setRowCount(0);
        if (!authenticated || currentUserId <= 0) return;

        try {
            List<DispatchBackend.DriverRecord> drivers = DispatchBackend.getDrivers(currentUserId);
            for (DispatchBackend.DriverRecord d : drivers) {
                driverModel.addRow(new Object[]{
                        "#" + d.driverId,
                        d.name,
                        d.phone,
                        d.area,
                        d.available ? "Yes" : "No",
                        "Toggle Status →"
                });
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    // ====================================================
    // ORDERS PANEL (Simplified initial view + Details Popup)
    // ====================================================
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        ModernComponents.RoundedCard card = new ModernComponents.RoundedCard(16);
        card.setLayout(new BorderLayout(12, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Simplified, clean initial columns - eliminates Excel sheet look
        orderModel = new DefaultTableModel(
                new String[]{
                        "Order ID", "Customer", "Delivery Area",
                        "Delivery Schedule", "Status", "Quick Action"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        orderTable = new JTable(orderModel);
        styleTable(orderTable);
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        orderTable.getColumnModel().getColumn(1).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(170);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(4).setCellRenderer(new ModernComponents.StatusBadgeRenderer());
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new ModernComponents.ActionLinkRenderer());

        // Row Click or Action Click triggers full Details Popup
        orderTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = orderTable.rowAtPoint(e.getPoint());
                int viewCol = orderTable.columnAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    int modelRow = orderTable.convertRowIndexToModel(viewRow);
                    if (e.getClickCount() == 2 || viewCol == 5) {
                        openOrderDetails(modelRow);
                    }
                }
            }
        });

        // Enable sorting and filtering
        orderSorter = new TableRowSorter<>(orderModel);
        orderTable.setRowSorter(orderSorter);

        // Search field with placeholder
        orderSearchField = new ModernComponents.ModernTextField(
                "Search customer, area, or date...", 22
        );

        // Status dropdown filter
        orderStatusFilter = new JComboBox<>(new String[]{
                "All Statuses",
                "Pending",
                "Assigned",
                "In Progress",
                "Delivered",
                "Failed"
        });
        ModernComponents.styleComboBox(orderStatusFilter);

        // Header and Filter Container (Two-tier clean layout matching Design 1)
        JPanel topContainer = new JPanel();
        topContainer.setOpaque(false);
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        // Row 1: Section Title & Action Buttons
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel sectionTitle = new JLabel("Orders Management");
        sectionTitle.setFont(UITheme.FONT_SECTION);
        sectionTitle.setForeground(PRIMARY);

        JLabel hint = new JLabel("Click any row or 'View Details' for address and actions");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(MUTED);

        titleBlock.add(sectionTitle);
        titleBlock.add(hint);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        ModernComponents.ModernButton add = new ModernComponents.ModernButton(
                "+ Add Order", ModernComponents.ButtonStyle.PRIMARY
        );
        ModernComponents.ModernButton assign = new ModernComponents.ModernButton(
                "Smart Assign", ModernComponents.ButtonStyle.SECONDARY
        );
        ModernComponents.ModernButton edit = new ModernComponents.ModernButton(
                "Edit Order", ModernComponents.ButtonStyle.OUTLINE
        );
        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh", ModernComponents.ButtonStyle.GHOST
        );

        add.addActionListener(e -> addOrder());
        refresh.addActionListener(e -> refreshAll());
        assign.addActionListener(e -> assignSelectedOrder());
        edit.addActionListener(e -> editSelectedOrder());

        actionButtons.add(add);
        actionButtons.add(assign);
        actionButtons.add(edit);
        actionButtons.add(refresh);

        row1.add(titleBlock, BorderLayout.CENTER);
        row1.add(actionButtons, BorderLayout.EAST);

        // Row 2: Search input & Status Filter
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row2.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(UITheme.FONT_BODY_BOLD);
        searchLabel.setForeground(PRIMARY);

        JLabel filterLabel = new JLabel("Status:");
        filterLabel.setFont(UITheme.FONT_BODY_BOLD);
        filterLabel.setForeground(PRIMARY);

        row2.add(searchLabel);
        row2.add(orderSearchField);
        row2.add(Box.createHorizontalStrut(12));
        row2.add(filterLabel);
        row2.add(orderStatusFilter);

        topContainer.add(row1);
        topContainer.add(row2);

        card.add(topContainer, BorderLayout.NORTH);

        // Apply filters when typing
        orderSearchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        applyOrderFilters();
                    }

                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        applyOrderFilters();
                    }

                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        applyOrderFilters();
                    }
                }
        );

        orderStatusFilter.addActionListener(e -> applyOrderFilters());

        JScrollPane scroll = new JScrollPane(orderTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    private void openOrderDetails(int modelRow) {
        if (modelRow < 0 || modelRow >= orderRecords.size()) return;
        OrderRecord rec = orderRecords.get(modelRow);
        DetailsDialogs.showOrderDetails(
                this,
                rec.orderId,
                rec.customer,
                rec.address,
                rec.area,
                rec.deliveryDate,
                rec.timeSlot,
                rec.status,
                rec.failureReason,
                () -> editSpecificOrder(rec),
                () -> assignSpecificOrder(rec.orderId, rec.area)
        );
    }

    private void addOrder() {
        JTextField customer = new ModernComponents.ModernTextField("e.g. Jane Doe", 15);
        JTextField address = new ModernComponents.ModernTextField("e.g. 123 Elm Street", 15);
        JTextField area = new ModernComponents.ModernTextField("e.g. North Side", 15);
        JTextField date = new ModernComponents.ModernTextField("YYYY-MM-DD", 15);
        date.setText("2026-11-01");

        JComboBox<String> slot = new JComboBox<>(
                new String[]{"9 AM - 12 PM", "12 PM - 3 PM", "3 PM - 6 PM"}
        );
        ModernComponents.styleComboBox(slot);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setOpaque(false);
        form.add(new JLabel("Customer Name *:"));
        form.add(customer);
        form.add(new JLabel("Delivery Address *:"));
        form.add(address);
        form.add(new JLabel("Delivery Area *:"));
        form.add(area);
        form.add(new JLabel("Date (YYYY-MM-DD) *:"));
        form.add(date);
        form.add(new JLabel("Time Slot:"));
        form.add(slot);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Create New Delivery Order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        if (customer.getText().isBlank() ||
                address.getText().isBlank() ||
                area.getText().isBlank()) {
            showError("Please fill all required fields.");
            return;
        }

        try {
            java.time.LocalDate.parse(date.getText().trim());
        } catch (java.time.format.DateTimeParseException ex) {
            showError("Enter a valid date in YYYY-MM-DD format.");
            return;
        }

        try {
            DispatchBackend.createOrder(
                    currentUserId,
                    customer.getText().trim(),
                    address.getText().trim(),
                    area.getText().trim(),
                    date.getText().trim(),
                    slot.getSelectedItem().toString()
            );

            JOptionPane.showMessageDialog(this, "Order created successfully!");
            refreshAll();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void assignSelectedOrder() {
        int selectedViewRow = orderTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an order first."
            );
            return;
        }

        int selectedRow =
                orderTable.convertRowIndexToModel(selectedViewRow);

        if (selectedRow < 0 || selectedRow >= orderRecords.size()) return;
        OrderRecord rec = orderRecords.get(selectedRow);

        if (!rec.status.equals("Pending")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only pending orders can be assigned."
            );
            return;
        }

        assignSpecificOrder(rec.orderId, rec.area);
    }

    private void assignSpecificOrder(int orderId, String area) {
        try {
            DispatchBackend.AssignmentResult res = DispatchBackend.smartAssignOrder(currentUserId, orderId, area);
            if (!res.success) {
                JOptionPane.showMessageDialog(this, res.message);
                return;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Driver assigned successfully!\n\n" +
                            "Driver: " + res.driverName +
                            "\nOrder ID: " + orderId +
                            "\nArea: " + area
            );

            refreshAll();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadOrders() {
        orderRecords.clear();
        orderModel.setRowCount(0);
        if (!authenticated || currentUserId <= 0) return;

        try {
            List<DispatchBackend.OrderRecord> orders = DispatchBackend.getOrders(currentUserId);
            for (DispatchBackend.OrderRecord item : orders) {
                OrderRecord rec = new OrderRecord();
                rec.orderId = item.orderId;
                rec.customer = item.customer;
                rec.address = item.address;
                rec.area = item.area;
                rec.deliveryDate = item.deliveryDate;
                rec.timeSlot = item.timeSlot;
                rec.status = item.status;
                rec.failureReason = item.failureReason;
                orderRecords.add(rec);

                orderModel.addRow(new Object[]{
                        "#" + rec.orderId,
                        rec.customer,
                        rec.area,
                        rec.deliveryDate + " · " + rec.timeSlot,
                        rec.status,
                        "View Details →"
                });
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshDashboard() {
        if (!authenticated || currentUserId <= 0) return;
        try {
            DispatchBackend.DashboardStats stats = DispatchBackend.getDashboardStats(currentUserId);

            // Update main metrics
            totalOrders.setText(String.valueOf(stats.totalOrders));
            pendingOrders.setText(String.valueOf(stats.pendingOrders));
            deliveredOrders.setText(String.valueOf(stats.deliveredOrders));
            inProgressOrders.setText(String.valueOf(stats.inProgressOrders + stats.assignedOrders));

            // Keep background metric counters updated
            failedOrders.setText(String.valueOf(stats.failedOrders));
            assignedOrders.setText(String.valueOf(stats.assignedOrders));
            totalDrivers.setText(String.valueOf(stats.totalDrivers));
            availableDrivers.setText(String.valueOf(stats.availableDrivers));

            if (analyticsPanel != null) {
                analyticsPanel.updateData(
                        stats.totalOrders,
                        stats.deliveredOrders,
                        stats.inProgressOrders,
                        stats.assignedOrders,
                        stats.pendingOrders,
                        stats.failedOrders,
                        stats.totalDrivers,
                        stats.availableDrivers
                );
            }

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        if (!authenticated || currentUserId <= 0) return;
        loadDrivers();
        loadOrders();
        loadAssignments();
        refreshDashboard();
        loadReportData();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "DispatchDesk Alert", JOptionPane.ERROR_MESSAGE
        );
    }

    private JButton createNavButton(String text) {
        return new ModernComponents.NavButton(text);
    }

    private void selectNavButton(
            JButton selected,
            JButton other1,
            JButton other2,
            JButton other3,
            JButton other4) {

        if (selected instanceof ModernComponents.NavButton nb) {
            nb.setActive(true);
        }
        for (JButton other : new JButton[]{other1, other2, other3, other4}) {
            if (other instanceof ModernComponents.NavButton nb) {
                nb.setActive(false);
            }
        }
    }

    private void styleButton(JButton button) {
        button.setFont(UITheme.FONT_BUTTON);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(PRIMARY);
            }
        });
    }

    private void styleTable(JTable table) {
        ModernComponents.styleModernTable(table);
    }

    private void applyTheme() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", UITheme.FONT_BODY);

        UIManager.put("Button.background", PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", UITheme.FONT_BUTTON);

        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", SIDEBAR_ACTIVE);
        UIManager.put("Table.selectionForeground", PRIMARY);
        UIManager.put("Table.gridColor", new Color(242, 238, 252));
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", PRIMARY);
        UIManager.put("TableHeader.font", UITheme.FONT_BODY_BOLD);

        UIManager.put("ScrollPane.background", CARD);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Viewport.background", CARD);

        UIManager.put("TextField.background", CARD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.font", UITheme.FONT_BODY);

        UIManager.put("ComboBox.background", CARD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SIDEBAR_ACTIVE);
        UIManager.put("ComboBox.selectionForeground", PRIMARY);
    }

    private void applyOrderFilters() {
        String searchText = orderSearchField.getText().trim();
        String selectedStatus =
                (String) orderStatusFilter.getSelectedItem();

        java.util.List<RowFilter<Object, Object>> filters =
                new java.util.ArrayList<>();

        // Search customer and area
        if (!searchText.isEmpty() && !searchText.equals("Search customer, area, or date...")) {
            filters.add(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(searchText),
                    1, 2, 3
            ));
        }

        // Filter by selected status
        if (selectedStatus != null &&
                !selectedStatus.equals("All Statuses")) {

            filters.add(RowFilter.regexFilter(
                    "^" + java.util.regex.Pattern.quote(selectedStatus) + "$",
                    4
            ));
        }

        if (filters.isEmpty()) {
            orderSorter.setRowFilter(null);
        } else {
            orderSorter.setRowFilter(
                    RowFilter.andFilter(filters)
            );
        }
    }

    private void editSelectedOrder() {
        int selectedViewRow = orderTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an order to edit."
            );
            return;
        }

        int selectedRow =
                orderTable.convertRowIndexToModel(selectedViewRow);

        if (selectedRow < 0 || selectedRow >= orderRecords.size()) return;
        OrderRecord rec = orderRecords.get(selectedRow);
        editSpecificOrder(rec);
    }

    private void editSpecificOrder(OrderRecord rec) {
        if (!rec.status.equals("Pending")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only pending orders can be edited."
            );
            return;
        }

        JTextField customerField = new ModernComponents.ModernTextField(15);
        customerField.setText(rec.customer);
        JTextField addressField = new ModernComponents.ModernTextField(15);
        addressField.setText(rec.address);
        JTextField areaField = new ModernComponents.ModernTextField(15);
        areaField.setText(rec.area);
        JTextField dateField = new ModernComponents.ModernTextField(15);
        dateField.setText(rec.deliveryDate);
        JTextField timeField = new ModernComponents.ModernTextField(15);
        timeField.setText(rec.timeSlot);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setOpaque(false);

        form.add(new JLabel("Customer Name:"));
        form.add(customerField);

        form.add(new JLabel("Address:"));
        form.add(addressField);

        form.add(new JLabel("Area:"));
        form.add(areaField);

        form.add(new JLabel("Delivery Date:"));
        form.add(dateField);

        form.add(new JLabel("Time Slot:"));
        form.add(timeField);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Edit Order #" + rec.orderId,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newCustomer = customerField.getText().trim();
        String newAddress = addressField.getText().trim();
        String newArea = areaField.getText().trim();
        String newDate = dateField.getText().trim();
        String newTime = timeField.getText().trim();

        if (newCustomer.isEmpty() ||
                newAddress.isEmpty() ||
                newArea.isEmpty() ||
                newDate.isEmpty() ||
                newTime.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all fields."
            );
            return;
        }

        try {
            boolean updated = DispatchBackend.updatePendingOrder(
                    currentUserId,
                    rec.orderId,
                    newCustomer,
                    newAddress,
                    newArea,
                    newDate,
                    newTime
            );

            if (updated) {
                JOptionPane.showMessageDialog(
                        this,
                        "Order updated successfully!"
                );
                refreshAll();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Order could not be updated."
                );
            }

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private JPanel createReportCard(String title, JLabel valueLabel) {
        return new DashboardStatCard(
                title, valueLabel, "📊", UITheme.BG_CANVAS, PRIMARY, "Analytics"
        );
    }

    private void loadReportData() {
        if (!authenticated || currentUserId <= 0) return;

        try {
            DispatchBackend.ReportSummary summary = DispatchBackend.getReportSummary(currentUserId);

            reportTotalOrders.setText(String.valueOf(summary.totalOrders));
            reportDeliveredOrders.setText(String.valueOf(summary.deliveredOrders));
            reportFailedOrders.setText(String.valueOf(summary.failedOrders));
            reportSuccessRate.setText(String.format("%.1f%%", summary.successRate));

            reportAreaModel.setRowCount(0);
            for (ReportsAnalyticsChart.AreaStat stat : summary.areaStats) {
                double successRate = stat.total == 0
                        ? 0
                        : (stat.delivered * 100.0 / stat.total);

                reportAreaModel.addRow(new Object[]{
                        stat.area,
                        stat.total,
                        stat.delivered,
                        stat.failed,
                        String.format("%.1f%%", successRate)
                });
            }

            // Update the graphical chart
            if (reportsChart != null) {
                reportsChart.setData(
                        summary.areaStats,
                        summary.successRate,
                        summary.totalOrders,
                        summary.deliveredOrders,
                        summary.failedOrders
                );
            }

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        Database.initialize();
        BackendApiServer.start(8080);

        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}
