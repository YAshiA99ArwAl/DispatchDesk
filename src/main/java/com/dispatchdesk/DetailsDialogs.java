package com.dispatchdesk;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DetailsDialogs {

    // ==========================================
    // 1. ORDER DETAILS POPUP (Inspired by Reference Design 3)
    // ==========================================
    public static void showOrderDetails(Frame parent, int orderId, String customer, String address,
                                        String area, String date, String timeSlot, String status,
                                        String failureReason, Runnable onEdit, Runnable onAssign) {

        JDialog dialog = new JDialog(parent, "Order Details - #" + orderId, true);
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Top Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBlock.setOpaque(false);

        // Avatar circle
        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.ACCENT_LAVENDER);
                g2.fillOval(0, 0, 40, 40);
                g2.setColor(UITheme.PRIMARY_DARK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                String initial = customer.isEmpty() ? "O" : customer.substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                int x = (40 - fm.stringWidth(initial)) / 2;
                int y = (40 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40);
            }
        };

        JPanel textGroup = new JPanel();
        textGroup.setOpaque(false);
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Order #" + orderId);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY_DARK);

        JLabel subLabel = new JLabel(customer);
        subLabel.setFont(UITheme.FONT_BODY_BOLD);
        subLabel.setForeground(UITheme.TEXT_SECONDARY);

        textGroup.add(titleLabel);
        textGroup.add(Box.createVerticalStrut(2));
        textGroup.add(subLabel);

        titleBlock.add(avatar);
        titleBlock.add(textGroup);

        // Status badge on top right
        JLabel badge = createStatusBadge(status);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        // Center Content Body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        body.add(createFieldCard("Delivery Area", area));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldCard("Delivery Address", address));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldCard("Scheduled Delivery", date + "  (" + timeSlot + ")"));

        if (failureReason != null && !failureReason.isBlank()) {
            body.add(Box.createVerticalStrut(10));
            JPanel failCard = createFieldCard("Failure Reason", failureReason);
            failCard.setBackground(new Color(254, 235, 235));
            body.add(failCard);
        }

        // Bottom Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        if (status.equalsIgnoreCase("Pending")) {
            ModernComponents.ModernButton assignBtn = new ModernComponents.ModernButton(
                    "⚡ Smart Assign", ModernComponents.ButtonStyle.PRIMARY
            );
            assignBtn.addActionListener(e -> {
                dialog.dispose();
                if (onAssign != null) onAssign.run();
            });

            ModernComponents.ModernButton editBtn = new ModernComponents.ModernButton(
                    "✎ Edit Order", ModernComponents.ButtonStyle.SECONDARY
            );
            editBtn.addActionListener(e -> {
                dialog.dispose();
                if (onEdit != null) onEdit.run();
            });

            footer.add(assignBtn);
            footer.add(editBtn);
        }

        ModernComponents.ModernButton closeBtn = new ModernComponents.ModernButton(
                "Close", ModernComponents.ButtonStyle.OUTLINE
        );
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    // ==========================================
    // 2. ASSIGNMENT DETAILS POPUP
    // ==========================================
    public static void showAssignmentDetails(Frame parent, int orderId, String customer, String driver,
                                             String area, String status, String assignedAt,
                                             String failureReason, Runnable onUpdateStatus) {

        JDialog dialog = new JDialog(parent, "Assignment Details - Order #" + orderId, true);
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Top Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Assignment #" + orderId);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY_DARK);

        JLabel subLabel = new JLabel("Customer: " + customer);
        subLabel.setFont(UITheme.FONT_BODY_BOLD);
        subLabel.setForeground(UITheme.TEXT_SECONDARY);

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subLabel);

        JLabel badge = createStatusBadge(status);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        // Center Content Body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        body.add(createFieldCard("Assigned Driver", driver));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldCard("Delivery Coverage Area", area));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldCard("Dispatched Timestamp", assignedAt == null ? "N/A" : assignedAt));

        if (failureReason != null && !failureReason.isBlank()) {
            body.add(Box.createVerticalStrut(10));
            JPanel failCard = createFieldCard("Failure Reason", failureReason);
            failCard.setBackground(new Color(254, 235, 235));
            body.add(failCard);
        }

        // Bottom Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        if (!status.equalsIgnoreCase("Delivered") && !status.equalsIgnoreCase("Failed")) {
            ModernComponents.ModernButton updateBtn = new ModernComponents.ModernButton(
                    "Update Status", ModernComponents.ButtonStyle.PRIMARY
            );
            updateBtn.addActionListener(e -> {
                dialog.dispose();
                if (onUpdateStatus != null) onUpdateStatus.run();
            });
            footer.add(updateBtn);
        }

        ModernComponents.ModernButton closeBtn = new ModernComponents.ModernButton(
                "Close", ModernComponents.ButtonStyle.OUTLINE
        );
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private static JPanel createFieldCard(String labelText, String valueText) {
        JPanel panel = new JPanel(new BorderLayout(8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(UITheme.CARD_BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setBackground(UITheme.BG_CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(UITheme.TEXT_MUTED);

        JLabel value = new JLabel(valueText == null || valueText.isBlank() ? "—" : valueText);
        value.setFont(UITheme.FONT_BODY_BOLD);
        value.setForeground(UITheme.PRIMARY_DARK);

        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);

        return panel;
    }

    private static JLabel createStatusBadge(String status) {
        JLabel badge = new JLabel("● " + status) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(UITheme.FONT_BADGE);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        switch (status.toLowerCase()) {
            case "delivered" -> {
                badge.setBackground(UITheme.STATUS_DELIVERED_BG);
                badge.setForeground(UITheme.STATUS_DELIVERED_FG);
            }
            case "in progress" -> {
                badge.setBackground(UITheme.STATUS_PROGRESS_BG);
                badge.setForeground(UITheme.STATUS_PROGRESS_FG);
            }
            case "assigned" -> {
                badge.setBackground(UITheme.STATUS_ASSIGNED_BG);
                badge.setForeground(UITheme.STATUS_ASSIGNED_FG);
            }
            case "pending" -> {
                badge.setBackground(UITheme.STATUS_PENDING_BG);
                badge.setForeground(UITheme.STATUS_PENDING_FG);
            }
            case "failed" -> {
                badge.setBackground(UITheme.STATUS_FAILED_BG);
                badge.setForeground(UITheme.STATUS_FAILED_FG);
            }
            default -> {
                badge.setBackground(UITheme.ACCENT_LAVENDER);
                badge.setForeground(UITheme.PRIMARY_DARK);
            }
        }
        return badge;
    }
}
