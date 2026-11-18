package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.AreaStat;
import com.dispatchdesk.model.ReportSummary;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.DashboardStatCard;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.components.ReportsAnalyticsChart;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Reports & Fleet Analytics view with KPI cards, area comparison chart, ring gauge, and breakdown table.
 */
public class ReportsPanel extends JPanel {

    private final IntSupplier currentUserIdSupplier;
    private final Consumer<String> errorHandler;

    private final JLabel reportTotalOrders;
    private final JLabel reportDeliveredOrders;
    private final JLabel reportFailedOrders;
    private final JLabel reportSuccessRate;
    private final DefaultTableModel reportAreaModel;
    private final ReportsAnalyticsChart reportsChart;

    public ReportsPanel(IntSupplier currentUserIdSupplier, Consumer<String> errorHandler) {
        super(new BorderLayout());
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.errorHandler = errorHandler;

        setBackground(UITheme.BG_CANVAS);

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setBackground(UITheme.BG_CANVAS);
        scrollContainer.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Statistics Cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 16));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        reportTotalOrders = new JLabel("0");
        reportDeliveredOrders = new JLabel("0");
        reportFailedOrders = new JLabel("0");
        reportSuccessRate = new JLabel("0%");

        cards.add(new DashboardStatCard("Total Tracked", reportTotalOrders, "📦", UITheme.BG_CANVAS, UITheme.PRIMARY_DARK, "All-time volume"));
        cards.add(new DashboardStatCard("Delivered", reportDeliveredOrders, "✓", UITheme.STATUS_DELIVERED_BG, UITheme.STATUS_DELIVERED_FG, "Fulfilled orders"));
        cards.add(new DashboardStatCard("Failed", reportFailedOrders, "✕", UITheme.STATUS_FAILED_BG, UITheme.STATUS_FAILED_FG, "Delivery issues"));
        cards.add(new DashboardStatCard("Success Rate", reportSuccessRate, "📈", UITheme.ACCENT_LAVENDER, UITheme.PRIMARY_DARK, "Fulfillment ratio"));

        scrollContainer.add(cards);
        scrollContainer.add(Box.createVerticalStrut(18));

        // Graphical Chart Widget (Area Comparison Bar Chart + Donut Ring Gauge)
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
        ModernComponents.styleModernTable(areaTable);

        JScrollPane scroll = new JScrollPane(areaTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("Area-wise Performance Breakdown");
        tableTitle.setFont(UITheme.FONT_SECTION);
        tableTitle.setForeground(UITheme.PRIMARY_DARK);

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
        mainScroll.setBackground(UITheme.BG_CANVAS);
        mainScroll.getViewport().setBackground(UITheme.BG_CANVAS);
        mainScroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        mainScroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        add(mainScroll, BorderLayout.CENTER);
    }

    public void loadReportData() {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            ReportSummary summary = DispatchBackend.getReportSummary(userId);

            reportTotalOrders.setText(String.valueOf(summary.totalOrders));
            reportDeliveredOrders.setText(String.valueOf(summary.deliveredOrders));
            reportFailedOrders.setText(String.valueOf(summary.failedOrders));
            reportSuccessRate.setText(String.format("%.1f%%", summary.successRate));

            reportAreaModel.setRowCount(0);
            for (AreaStat stat : summary.areaStats) {
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

            reportsChart.setData(
                    summary.areaStats,
                    summary.successRate,
                    summary.totalOrders,
                    summary.deliveredOrders,
                    summary.failedOrders
            );
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    public void clearData() {
        reportTotalOrders.setText("0");
        reportDeliveredOrders.setText("0");
        reportFailedOrders.setText("0");
        reportSuccessRate.setText("0%");
        reportAreaModel.setRowCount(0);
        reportsChart.setData(Collections.emptyList(), 0.0, 0, 0, 0);
    }
}
