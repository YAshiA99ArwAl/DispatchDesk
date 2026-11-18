package com.dispatchdesk;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DashboardStatCard extends ModernComponents.RoundedCard {
    private final String title;
    private final JLabel valueLabel;
    private final String iconSymbol;
    private final Color iconBg;
    private final Color iconFg;
    private final String subtitle;

    public DashboardStatCard(String title, JLabel valueLabel, String iconSymbol, Color iconBg, Color iconFg, String subtitle) {
        super(16);
        this.title = title;
        this.valueLabel = valueLabel;
        this.iconSymbol = iconSymbol;
        this.iconBg = iconBg;
        this.iconFg = iconFg;
        this.subtitle = subtitle;

        setLayout(new BorderLayout(10, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Top Header: Title on Center, Icon Pill on Right
        JPanel topPanel = new JPanel(new BorderLayout(4, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JComponent iconComp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                int sz = 32;
                g2.setColor(iconBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, sz, sz, 8, 8));

                g2.setColor(iconFg);
                g2.setStroke(new BasicStroke(1.8f));

                int midX = sz / 2;
                int midY = sz / 2;

                if (iconSymbol.equals("📦")) {
                    g2.drawRoundRect(midX - 7, midY - 6, 14, 12, 2, 2);
                    g2.drawLine(midX - 7, midY - 2, midX + 7, midY - 2);
                    g2.drawLine(midX, midY - 2, midX, midY + 6);
                } else if (iconSymbol.equals("⏳")) {
                    g2.drawOval(midX - 6, midY - 6, 12, 12);
                    g2.drawLine(midX, midY - 4, midX, midY);
                    g2.drawLine(midX, midY, midX + 3, midY);
                } else if (iconSymbol.equals("🔗")) {
                    g2.drawOval(midX - 6, midY - 4, 8, 8);
                    g2.drawOval(midX - 2, midY - 4, 8, 8);
                } else if (iconSymbol.equals("🚚")) {
                    g2.drawRoundRect(midX - 8, midY - 4, 11, 8, 2, 2);
                    g2.drawRoundRect(midX + 3, midY - 2, 5, 6, 1, 1);
                    g2.drawOval(midX - 6, midY + 3, 4, 4);
                    g2.drawOval(midX + 2, midY + 3, 4, 4);
                } else if (iconSymbol.equals("✓")) {
                    g2.drawLine(midX - 6, midY, midX - 2, midY + 4);
                    g2.drawLine(midX - 2, midY + 4, midX + 6, midY - 4);
                } else if (iconSymbol.equals("✕")) {
                    g2.drawLine(midX - 5, midY - 5, midX + 5, midY + 5);
                    g2.drawLine(midX + 5, midY - 5, midX - 5, midY + 5);
                } else if (iconSymbol.equals("👥")) {
                    g2.drawOval(midX - 5, midY - 6, 6, 6);
                    g2.drawArc(midX - 8, midY, 12, 8, 0, 180);
                } else if (iconSymbol.equals("⚡")) {
                    int[] xPoints = {midX + 1, midX - 4, midX - 1, midX - 3, midX + 4, midX + 1, midX + 5};
                    int[] yPoints = {midY - 7, midY - 1, midY - 1, midY + 7, midY + 1, midY + 1, midY - 7};
                    g2.fillPolygon(xPoints, yPoints, 7);
                } else if (iconSymbol.equals("📊")) {
                    g2.fillRect(midX - 6, midY + 2, 3, 5);
                    g2.fillRect(midX - 2, midY - 2, 3, 9);
                    g2.fillRect(midX + 2, midY - 6, 3, 13);
                } else {
                    g2.fillOval(midX - 4, midY - 4, 8, 8);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }
        };

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(iconComp, BorderLayout.EAST);

        // Center: Metric value
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        // Bottom: Subtitle
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 2));
        centerPanel.setOpaque(false);
        centerPanel.add(valueLabel, BorderLayout.CENTER);
        centerPanel.add(subLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(190, 115);
    }
}
