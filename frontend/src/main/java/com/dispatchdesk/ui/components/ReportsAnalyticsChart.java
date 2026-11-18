package com.dispatchdesk.ui.components;

import com.dispatchdesk.model.AreaStat;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ReportsAnalyticsChart extends ModernComponents.RoundedCard {

    private final List<AreaStat> areaStats = new ArrayList<>();
    private double overallSuccessRate = 0.0;
    private int totalOrders = 0;
    private int deliveredOrders = 0;
    private int failedOrders = 0;

    public ReportsAnalyticsChart() {
        super(16);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        setPreferredSize(new Dimension(0, 240));
    }

    public void setData(List<AreaStat> stats, double successRate, int total, int delivered, int failed) {
        this.areaStats.clear();
        this.areaStats.addAll(stats);
        this.overallSuccessRate = successRate;
        this.totalOrders = total;
        this.deliveredOrders = delivered;
        this.failedOrders = failed;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.enableAntiAliasing(g2);

        int totalW = getWidth();
        int totalH = getHeight();

        int chartW = (int) (totalW * 0.64);
        int ringX = chartW + 24;
        int ringW = totalW - ringX - 24;

        // Divider between bar chart and ring chart
        g2.setColor(new Color(242, 238, 252));
        g2.drawLine(chartW + 12, 18, chartW + 12, totalH - 18);

        // ==========================================
        // 1. LEFT: AREA COMPARISON BAR CHART
        // ==========================================
        g2.setFont(UITheme.FONT_SECTION);
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.drawString("Area Fulfillment Comparison", 24, 28);

        // Legend at top right of bar chart area
        int legX = chartW - 240;
        int legY = 24;
        drawLegendBox(g2, legX, legY, UITheme.PRIMARY_DARK, "Total");
        drawLegendBox(g2, legX + 70, legY, UITheme.ACCENT_PERIWINKLE, "Delivered");
        drawLegendBox(g2, legX + 160, legY, UITheme.ACCENT_LAVENDER, "Failed");

        // Chart plot area
        int plotX = 36;
        int plotY = 52;
        int plotW = chartW - 48;
        int plotH = totalH - 96;
        int baselineY = plotY + plotH;

        g2.setColor(new Color(244, 240, 252));
        g2.drawLine(plotX, baselineY, plotX + plotW, baselineY);
        g2.drawLine(plotX, plotY + (plotH / 2), plotX + plotW, plotY + (plotH / 2));

        if (!areaStats.isEmpty()) {
            int maxVal = 1;
            for (AreaStat as : areaStats) {
                if (as.total > maxVal) maxVal = as.total;
            }

            int groupCount = areaStats.size();
            int groupSlotW = plotW / groupCount;
            int barW = Math.max(10, Math.min(22, (groupSlotW - 20) / 3));

            for (int i = 0; i < groupCount; i++) {
                AreaStat as = areaStats.get(i);
                int groupCenterX = plotX + (i * groupSlotW) + (groupSlotW / 2);
                int startBarX = groupCenterX - ((barW * 3 + 4) / 2);

                drawBar(g2, startBarX, baselineY, barW, plotH, as.total, maxVal, UITheme.PRIMARY_DARK);
                drawBar(g2, startBarX + barW + 2, baselineY, barW, plotH, as.delivered, maxVal, UITheme.ACCENT_PERIWINKLE);
                drawBar(g2, startBarX + (barW * 2) + 4, baselineY, barW, plotH, as.failed, maxVal, UITheme.ACCENT_LAVENDER);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(UITheme.TEXT_SECONDARY);
                FontMetrics fm = g2.getFontMetrics();
                String areaLabel = as.area.length() > 10 ? as.area.substring(0, 8) + ".." : as.area;
                int nameX = groupCenterX - (fm.stringWidth(areaLabel) / 2);
                g2.drawString(areaLabel, nameX, baselineY + 18);
            }
        } else {
            g2.setFont(UITheme.FONT_BODY);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("No area records currently loaded.", plotX + 20, plotY + 40);
        }

        // ==========================================
        // 2. RIGHT: SUCCESS RATE CIRCULAR RING GAUGE
        // ==========================================
        int ringCenterX = ringX + (ringW / 2);
        int ringCenterY = 96;
        int ringDiameter = 100;
        int ringRadius = ringDiameter / 2;
        int ringThickness = 12;

        g2.setFont(UITheme.FONT_SECTION);
        g2.setColor(UITheme.PRIMARY_DARK);
        String ringTitle = "Success Ratio";
        FontMetrics fmT = g2.getFontMetrics();
        g2.drawString(ringTitle, ringCenterX - (fmT.stringWidth(ringTitle) / 2), 28);

        g2.setColor(UITheme.ACCENT_LAVENDER);
        g2.setStroke(new BasicStroke(ringThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval(ringCenterX - ringRadius, ringCenterY - ringRadius, ringDiameter, ringDiameter);

        if (overallSuccessRate > 0) {
            g2.setColor(UITheme.PRIMARY_DARK);
            double extent = -(overallSuccessRate / 100.0) * 360.0;
            g2.draw(new Arc2D.Double(ringCenterX - ringRadius, ringCenterY - ringRadius, ringDiameter, ringDiameter, 90, extent, Arc2D.OPEN));
        }

        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.setColor(UITheme.PRIMARY_DARK);
        String pctStr = String.format("%.1f%%", overallSuccessRate);
        FontMetrics fmPct = g2.getFontMetrics();
        g2.drawString(pctStr, ringCenterX - (fmPct.stringWidth(pctStr) / 2), ringCenterY + (fmPct.getAscent() / 2) - 2);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(UITheme.TEXT_MUTED);
        String sub = totalOrders + " Total · " + deliveredOrders + " Delivered";
        FontMetrics fmSub = g2.getFontMetrics();
        g2.drawString(sub, ringCenterX - (fmSub.stringWidth(sub) / 2), ringCenterY + ringRadius + 24);

        g2.dispose();
    }

    private void drawBar(Graphics2D g2, int x, int baselineY, int w, int maxH, int val, int maxVal, Color c) {
        if (val <= 0) return;
        int barH = (int) Math.round(((double) val / maxVal) * (maxH - 24));
        if (barH < 4) barH = 4;
        int y = baselineY - barH;

        g2.setColor(c);
        g2.fill(new RoundRectangle2D.Float(x, y, w, barH, 4, 4));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(UITheme.PRIMARY_DARK);
        FontMetrics fm = g2.getFontMetrics();
        String vStr = String.valueOf(val);
        g2.drawString(vStr, x + (w - fm.stringWidth(vStr)) / 2, y - 3);
    }

    private void drawLegendBox(Graphics2D g2, int x, int y, Color c, String text) {
        g2.setColor(c);
        g2.fillRoundRect(x, y - 8, 8, 8, 2, 2);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(UITheme.TEXT_SECONDARY);
        g2.drawString(text, x + 12, y);
    }
}
