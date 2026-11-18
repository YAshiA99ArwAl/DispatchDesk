package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.DashboardStats;
import com.dispatchdesk.model.UserSession;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.DashboardAnalyticsPanel;
import com.dispatchdesk.ui.components.DashboardStatCard;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Operational Dashboard view showing 4 primary KPI cards and the live delivery pipeline.
 */
public class DashboardPanel extends JPanel {

    private final IntSupplier currentUserIdSupplier;
    private final Consumer<String> errorHandler;

    private final JLabel subtitleLabel;
    private final JLabel totalOrders;
    private final JLabel pendingOrders;
    private final JLabel inProgressOrders;
    private final JLabel deliveredOrders;
    private final JLabel assignedOrders;
    private final JLabel failedOrders;
    private final JLabel totalDrivers;
    private final JLabel availableDrivers;
    private final DashboardAnalyticsPanel analyticsPanel;

    public DashboardPanel(IntSupplier currentUserIdSupplier, Consumer<String> errorHandler) {
        super(new BorderLayout());
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.errorHandler = errorHandler;

        setBackground(UITheme.BG_CANVAS);

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setBackground(UITheme.BG_CANVAS);
        scrollContainer.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header Title within Dashboard
        JPanel dashHeader = new JPanel(new BorderLayout());
        dashHeader.setOpaque(false);
        dashHeader.setPreferredSize(new Dimension(0, 48));
        dashHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel title = new JLabel("Operational Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(UITheme.PRIMARY_DARK);

        subtitleLabel = new JLabel("Essential delivery metrics and fleet readiness summary");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitleLabel);

        dashHeader.add(titleBlock, BorderLayout.WEST);
        scrollContainer.add(dashHeader);
        scrollContainer.add(Box.createVerticalStrut(20));

        // Main 4 KPI Metric Cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 18, 18));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        totalOrders = new JLabel("0");
        pendingOrders = new JLabel("0");
        inProgressOrders = new JLabel("0");
        deliveredOrders = new JLabel("0");

        assignedOrders = new JLabel("0");
        failedOrders = new JLabel("0");
        totalDrivers = new JLabel("0");
        availableDrivers = new JLabel("0");

        cards.add(new DashboardStatCard("Total Orders", totalOrders, "📦", UITheme.BG_CANVAS, UITheme.PRIMARY_DARK, "All registered orders"));
        cards.add(new DashboardStatCard("Pending Action", pendingOrders, "⏳", UITheme.STATUS_PENDING_BG, UITheme.STATUS_PENDING_FG, "Awaiting assignment"));
        cards.add(new DashboardStatCard("In Transit", inProgressOrders, "🚚", UITheme.ACCENT_LAVENDER, UITheme.PRIMARY_DARK, "Active deliveries"));
        cards.add(new DashboardStatCard("Delivered", deliveredOrders, "✓", UITheme.STATUS_DELIVERED_BG, UITheme.STATUS_DELIVERED_FG, "Completed fulfillment"));

        scrollContainer.add(cards);
        scrollContainer.add(Box.createVerticalStrut(22));

        // Delivery Pipeline & Fleet Breakdown Card
        analyticsPanel = new DashboardAnalyticsPanel();
        analyticsPanel.setPreferredSize(new Dimension(0, 390));
        analyticsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 410));
        scrollContainer.add(analyticsPanel);
        scrollContainer.add(Box.createVerticalStrut(18));

        // Bottom Actions & Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        bottomBar.setPreferredSize(new Dimension(0, 46));

        JPanel statusInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        statusInfo.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(30, 130, 60));
        dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel statusText = new JLabel("All dispatch services operational · Real-time pipeline active");
        statusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.setForeground(UITheme.TEXT_SECONDARY);
        statusInfo.add(dot);
        statusInfo.add(statusText);

        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh Dashboard", ModernComponents.ButtonStyle.PRIMARY
        );
        refresh.addActionListener(e -> refreshData());

        bottomBar.add(statusInfo, BorderLayout.WEST);
        bottomBar.add(refresh, BorderLayout.EAST);

        scrollContainer.add(bottomBar);

        JScrollPane scrollPane = new JScrollPane(scrollContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UITheme.BG_CANVAS);
        scrollPane.getViewport().setBackground(UITheme.BG_CANVAS);
        scrollPane.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        add(scrollPane, BorderLayout.CENTER);
    }

    public void setUserSession(UserSession session) {
        if (session == null) return;
        subtitleLabel.setText(
                "Workspace for " + session.fullName + " (@" + session.username + ") · Essential delivery metrics and fleet readiness summary"
        );
    }

    public void refreshData() {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            DashboardStats stats = DispatchBackend.getDashboardStats(userId);

            totalOrders.setText(String.valueOf(stats.totalOrders));
            pendingOrders.setText(String.valueOf(stats.pendingOrders));
            deliveredOrders.setText(String.valueOf(stats.deliveredOrders));
            inProgressOrders.setText(String.valueOf(stats.inProgressOrders + stats.assignedOrders));

            failedOrders.setText(String.valueOf(stats.failedOrders));
            assignedOrders.setText(String.valueOf(stats.assignedOrders));
            totalDrivers.setText(String.valueOf(stats.totalDrivers));
            availableDrivers.setText(String.valueOf(stats.availableDrivers));

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
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    public void clearData() {
        totalOrders.setText("0");
        pendingOrders.setText("0");
        inProgressOrders.setText("0");
        deliveredOrders.setText("0");
        assignedOrders.setText("0");
        failedOrders.setText("0");
        totalDrivers.setText("0");
        availableDrivers.setText("0");
        analyticsPanel.updateData(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
