package com.dispatchdesk.ui;

import com.dispatchdesk.model.UserSession;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.panels.AssignmentsPanel;
import com.dispatchdesk.ui.panels.DashboardPanel;
import com.dispatchdesk.ui.panels.DriversPanel;
import com.dispatchdesk.ui.panels.LoginPanel;
import com.dispatchdesk.ui.panels.OrdersPanel;
import com.dispatchdesk.ui.panels.ReportsPanel;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Primary DispatchDesk desktop window managing session routing, sidebar navigation,
 * and coordination across all feature panels.
 */
public class MainFrame extends JFrame {

    private static final Color SIDEBAR_ACTIVE = UITheme.ACCENT_LAVENDER;
    private static final Color BACKGROUND = UITheme.BG_CANVAS;
    private static final Color CARD = UITheme.CARD_BG;
    private static final Color PRIMARY = UITheme.PRIMARY_DARK;
    private static final Color TEXT = UITheme.TEXT_PRIMARY;
    private static final Color MUTED = UITheme.TEXT_SECONDARY;
    private static final Color BORDER = UITheme.CARD_BORDER;

    // Authentication & Session State (Per-User Data Isolation)
    private boolean authenticated = false;
    private int currentUserId = -1;
    private String currentUsername = "";
    private String currentFullName = "";

    private final CardLayout mainCardLayout;
    private final JPanel mainContainer;
    private final LoginPanel loginPanel;

    private DashboardPanel dashboardPanel;
    private OrdersPanel ordersPanel;
    private DriversPanel driversPanel;
    private AssignmentsPanel assignmentsPanel;
    private ReportsPanel reportsPanel;

    private JLabel userNameLabel;
    private JComponent userAvatarComponent;
    private CardLayout pagesCardLayout;
    private JPanel pagesContainer;
    private JLabel pageHeadingLabel;
    private JButton dashboardNavBtn;
    private JButton ordersNavBtn;
    private JButton driversNavBtn;
    private JButton assignmentsNavBtn;
    private JButton reportsNavBtn;

    public MainFrame() {
        applyTheme();

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
        JPanel appPanel = createAppPanel();

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(appPanel, "APP");

        add(mainContainer);

        // Route Protection: Unauthenticated users start directly on LOGIN page
        mainCardLayout.show(mainContainer, "LOGIN");
        loginPanel.focusInitial();
    }

    private int getActiveUserId() {
        return authenticated ? currentUserId : -1;
    }

    private void onLoginSuccess(UserSession session) {
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
        if (dashboardPanel != null) {
            dashboardPanel.setUserSession(session);
        }
        if (ordersPanel != null) {
            ordersPanel.resetFilters();
        }

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
        if (dashboardPanel != null) dashboardPanel.clearData();
        if (ordersPanel != null) ordersPanel.clearData();
        if (driversPanel != null) driversPanel.clearData();
        if (assignmentsPanel != null) assignmentsPanel.clearData();
        if (reportsPanel != null) reportsPanel.clearData();
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
        // LEFT SIDEBAR
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

        // Feature Panels
        dashboardPanel = new DashboardPanel(this::getActiveUserId, this::showError);
        ordersPanel = new OrdersPanel(this, this::getActiveUserId, this::refreshAll, this::showError);
        driversPanel = new DriversPanel(this, this::getActiveUserId, this::refreshAll, this::showError);
        assignmentsPanel = new AssignmentsPanel(this, this::getActiveUserId, this::refreshAll, this::showError);
        reportsPanel = new ReportsPanel(this::getActiveUserId, this::showError);

        JPanel pages = new JPanel(new CardLayout());
        pages.setBackground(BACKGROUND);
        this.pagesContainer = pages;

        pages.add(dashboardPanel, "Dashboard");
        pages.add(ordersPanel, "Orders");
        pages.add(driversPanel, "Drivers");
        pages.add(assignmentsPanel, "Assignments");
        pages.add(reportsPanel, "Reports");

        CardLayout layout = (CardLayout) pages.getLayout();
        this.pagesCardLayout = layout;

        // Navigation Buttons
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
            reportsPanel.loadReportData();
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

        // Sidebar Footer: User Profile & Logout
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

    public void refreshAll() {
        if (!authenticated || currentUserId <= 0) return;
        driversPanel.loadDrivers();
        ordersPanel.loadOrders();
        assignmentsPanel.loadAssignments();
        dashboardPanel.refreshData();
        reportsPanel.loadReportData();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "DispatchDesk Alert", JOptionPane.ERROR_MESSAGE
        );
    }

    private void selectNavButton(JButton selected, JButton... others) {
        if (selected instanceof ModernComponents.NavButton nb) {
            nb.setActive(true);
        }
        for (JButton other : others) {
            if (other instanceof ModernComponents.NavButton nb) {
                nb.setActive(false);
            }
        }
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
}
