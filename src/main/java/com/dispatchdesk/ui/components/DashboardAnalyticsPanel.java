package com.dispatchdesk.ui.components;

import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Expanded, modern operational pipeline and fleet overview panel.
 * Fills the dashboard vertically with rich, balanced SaaS data presentation.
 */
public class DashboardAnalyticsPanel extends ModernComponents.RoundedCard {
    private int totalOrders = 0;
    private int delivered = 0;
    private int inProgress = 0;
    private int assigned = 0;
    private int pending = 0;
    private int failed = 0;

    private int totalDrivers = 0;
    private int availableDrivers = 0;

    public DashboardAnalyticsPanel() {
        super(16);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 390));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 410));
    }

    public void updateData(int total, int delivered, int inProgress, int assigned, int pending, int failed,
                           int drivers, int available) {
        this.totalOrders = total;
        this.delivered = delivered;
        this.inProgress = inProgress;
        this.assigned = assigned;
        this.pending = pending;
        this.failed = failed;
        this.totalDrivers = drivers;
        this.availableDrivers = available;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.enableAntiAliasing(g2);

        int cardW = getWidth();
        int w = cardW - 56;
        int startX = 28;
        int currentY = 32;

        // ==========================================
        // 1. HEADER: Section Title & Live Status Badge
        // ==========================================
        g2.setFont(UITheme.FONT_SECTION);
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.drawString("Delivery Pipeline & Fleet Overview", startX, currentY);

        String liveBadge = "● Live Dispatch Sync";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int badgeW = fm.stringWidth(liveBadge) + 20;
        int badgeX = startX + w - badgeW;
        int badgeY = currentY - 14;

        g2.setColor(new Color(232, 240, 254));
        g2.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, 22, 11, 11));
        g2.setColor(new Color(26, 86, 219));
        g2.drawString(liveBadge, badgeX + 10, badgeY + 15);

        // Subtitle
        currentY += 18;
        g2.setFont(UITheme.FONT_SUBTITLE);
        g2.setColor(UITheme.TEXT_SECONDARY);
        g2.drawString("Real-time fulfillment progress across active delivery cycles", startX, currentY);

        // ==========================================
        // 2. MULTI-SEGMENT DELIVERY PIPELINE BAR
        // ==========================================
        currentY += 20;
        int barHeight = 16;
        int arc = 8;

        g2.setColor(UITheme.BG_CANVAS);
        g2.fill(new RoundRectangle2D.Float(startX, currentY, w, barHeight, arc, arc));

        if (totalOrders > 0) {
            float xOffset = startX;

            if (delivered > 0) {
                float segW = ((float) delivered / totalOrders) * w;
                g2.setColor(UITheme.PIPELINE_DELIVERED);
                g2.fill(new RoundRectangle2D.Float(xOffset, currentY, segW, barHeight, arc, arc));
                xOffset += segW;
            }
            if (inProgress > 0) {
                float segW = ((float) inProgress / totalOrders) * w;
                g2.setColor(UITheme.PIPELINE_PROGRESS);
                g2.fill(new RoundRectangle2D.Float(xOffset, currentY, segW, barHeight, arc, arc));
                xOffset += segW;
            }
            if (assigned > 0) {
                float segW = ((float) assigned / totalOrders) * w;
                g2.setColor(UITheme.PIPELINE_ASSIGNED);
                g2.fill(new RoundRectangle2D.Float(xOffset, currentY, segW, barHeight, arc, arc));
                xOffset += segW;
            }
            if (pending > 0) {
                float segW = ((float) pending / totalOrders) * w;
                g2.setColor(UITheme.PIPELINE_PENDING);
                g2.fill(new RoundRectangle2D.Float(xOffset, currentY, segW, barHeight, arc, arc));
                xOffset += segW;
            }
            if (failed > 0) {
                float segW = ((float) failed / totalOrders) * w;
                g2.setColor(UITheme.PIPELINE_FAILED);
                g2.fill(new RoundRectangle2D.Float(xOffset, currentY, segW, barHeight, arc, arc));
            }
        }

        // ==========================================
        // 3. 5 DETAILED STAGE CARDS / TILES
        // ==========================================
        currentY += barHeight + 16;
        int tileGap = 12;
        int tileW = (w - (4 * tileGap)) / 5;
        int tileH = 58;

        drawStageTile(g2, startX, currentY, tileW, tileH, "Delivered", delivered,
                totalOrders, UITheme.PIPELINE_DELIVERED, new Color(244, 245, 252));
        drawStageTile(g2, startX + tileW + tileGap, currentY, tileW, tileH, "In Progress", inProgress,
                totalOrders, UITheme.PIPELINE_PROGRESS, new Color(245, 246, 253));
        drawStageTile(g2, startX + (tileW + tileGap) * 2, currentY, tileW, tileH, "Assigned", assigned,
                totalOrders, UITheme.PIPELINE_ASSIGNED, new Color(246, 247, 254));
        drawStageTile(g2, startX + (tileW + tileGap) * 3, currentY, tileW, tileH, "Pending", pending,
                totalOrders, UITheme.PIPELINE_PENDING, new Color(250, 248, 254));
        drawStageTile(g2, startX + (tileW + tileGap) * 4, currentY, tileW, tileH, "Issues/Failed", failed,
                totalOrders, UITheme.PIPELINE_FAILED, new Color(253, 248, 251));

        // ==========================================
        // 4. SUBTLE DIVIDER
        // ==========================================
        currentY += tileH + 18;
        g2.setColor(UITheme.CARD_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(startX, currentY, startX + w, currentY);

        // ==========================================
        // 5. FLEET CAPACITY & DEPLOYMENT SECTION
        // ==========================================
        currentY += 22;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.drawString("Fleet Capacity & Driver Allocation", startX, currentY);

        int percentAvailable = totalDrivers > 0 ? (int) Math.round((availableDrivers * 100.0) / totalDrivers) : 0;
        String pctStr = percentAvailable + "% Available (" + availableDrivers + " of " + totalDrivers + " on standby)";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fm = g2.getFontMetrics();
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.drawString(pctStr, startX + w - fm.stringWidth(pctStr), currentY);

        currentY += 12;
        int fleetBarH = 10;
        g2.setColor(UITheme.BG_CANVAS);
        g2.fill(new RoundRectangle2D.Float(startX, currentY, w, fleetBarH, 6, 6));

        if (totalDrivers > 0 && availableDrivers > 0) {
            float availW = ((float) availableDrivers / totalDrivers) * w;
            g2.setColor(UITheme.PRIMARY_DARK);
            g2.fill(new RoundRectangle2D.Float(startX, currentY, availW, fleetBarH, 6, 6));
        }

        currentY += fleetBarH + 14;
        int pillX = startX;
        int busyDrivers = Math.max(0, totalDrivers - availableDrivers);
        pillX = drawPill(g2, pillX, currentY, UITheme.STATUS_DELIVERED_FG, "Standby / Available: " + availableDrivers + " drivers");
        pillX = drawPill(g2, pillX, currentY, UITheme.ACCENT_PERIWINKLE.darker(), "Active on Route: " + busyDrivers + " drivers");
        drawPill(g2, pillX, currentY, UITheme.PRIMARY_DARK, "Total Fleet Size: " + totalDrivers + " registered");

        // ==========================================
        // 6. SYSTEM READINESS CHIPS (Bottom Bar of Card)
        // ==========================================
        currentY += 24;
        g2.setColor(UITheme.CARD_BORDER);
        g2.drawLine(startX, currentY, startX + w, currentY);

        currentY += 20;
        int chipX = startX;
        chipX = drawChip(g2, chipX, currentY, "SQLite Engine: Connected", new Color(30, 130, 60));
        chipX = drawChip(g2, chipX, currentY, "Route Optimization: Active", UITheme.PRIMARY_DARK);
        drawChip(g2, chipX, currentY, "Auto-Dispatch Rules: Operational", new Color(66, 72, 116));

        g2.dispose();
    }

    private void drawStageTile(Graphics2D g2, int x, int y, int width, int height,
                               String stageName, int count, int total, Color dotColor, Color bg) {
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(x, y, width, height, 10, 10));
        g2.setColor(new Color(230, 226, 244));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

        g2.setColor(dotColor);
        g2.fillOval(x + 10, y + 10, 8, 8);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(UITheme.TEXT_SECONDARY);
        g2.drawString(stageName, x + 22, y + 18);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.drawString(String.valueOf(count), x + 10, y + 44);

        int pct = total > 0 ? (int) Math.round((count * 100.0) / total) : 0;
        String pctStr = pct + "%";
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(UITheme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(pctStr, x + width - fm.stringWidth(pctStr) - 10, y + 44);
    }

    private int drawPill(Graphics2D g2, int x, int y, Color dotColor, String text) {
        g2.setColor(dotColor);
        g2.fillOval(x, y - 8, 8, 8);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(UITheme.TEXT_PRIMARY);
        g2.drawString(text, x + 14, y);
        FontMetrics fm = g2.getFontMetrics();
        return x + 14 + fm.stringWidth(text) + 26;
    }

    private int drawChip(Graphics2D g2, int x, int y, String text, Color fgColor) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int strW = fm.stringWidth(text);
        int padH = 6;
        int padW = 12;

        int boxW = strW + (padW * 2) + 14;
        int boxH = fm.getHeight() + (padH * 2);

        g2.setColor(UITheme.BG_CANVAS);
        g2.fill(new RoundRectangle2D.Float(x, y - fm.getAscent() - padH, boxW, boxH, 8, 8));
        g2.setColor(UITheme.CARD_BORDER);
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(new RoundRectangle2D.Float(x, y - fm.getAscent() - padH, boxW, boxH, 8, 8));

        g2.setColor(fgColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int checkX = x + padW;
        int checkY = y - 4;
        g2.drawLine(checkX, checkY, checkX + 3, checkY + 4);
        g2.drawLine(checkX + 3, checkY + 4, checkX + 8, checkY - 4);

        g2.drawString(text, x + padW + 13, y);
        return x + boxW + 14;
    }
}
