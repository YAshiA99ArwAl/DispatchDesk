package com.dispatchdesk;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ModernComponents {

    // ==========================================
    // 1. ROUNDED CARD PANEL
    // ==========================================
    public static class RoundedCard extends JPanel {
        private final int arc;
        private Color borderColor;

        public RoundedCard(int arc) {
            super();
            this.arc = arc;
            this.borderColor = UITheme.CARD_BORDER;
            setOpaque(false);
            setBackground(UITheme.CARD_BG);
        }

        public RoundedCard() {
            this(16);
        }

        public void setBorderColor(Color color) {
            this.borderColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            // Fill background
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            // Draw border
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ==========================================
    // 2. MODERN BUTTON
    // ==========================================
    public enum ButtonStyle {
        PRIMARY,    // Deep indigo #424874, white text
        SECONDARY,  // Periwinkle #A6B1E1, dark text #424874
        ACCENT,     // Lavender #DCD6F7, dark text #424874
        OUTLINE,    // White background, #DCD6F7 border, #424874 text
        GHOST       // Transparent background, hover #F4EEFF
    }

    public static class ModernButton extends JButton {
        private final ButtonStyle style;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private int arc = 12;

        public ModernButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setFont(UITheme.FONT_BUTTON);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    isPressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setArc(int arc) {
            this.arc = arc;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            Color bg;
            Color fg;
            Color border = null;

            switch (style) {
                case PRIMARY -> {
                    bg = isPressed ? UITheme.PRIMARY_DARK.darker() :
                         isHovered ? new Color(52, 57, 92) : UITheme.PRIMARY_DARK;
                    fg = Color.WHITE;
                }
                case SECONDARY -> {
                    bg = isPressed ? UITheme.ACCENT_PERIWINKLE.darker() :
                         isHovered ? new Color(150, 162, 215) : UITheme.ACCENT_PERIWINKLE;
                    fg = UITheme.PRIMARY_DARK;
                }
                case ACCENT -> {
                    bg = isPressed ? UITheme.ACCENT_LAVENDER.darker() :
                         isHovered ? new Color(208, 201, 240) : UITheme.ACCENT_LAVENDER;
                    fg = UITheme.PRIMARY_DARK;
                }
                case OUTLINE -> {
                    bg = isPressed ? UITheme.BG_CANVAS :
                         isHovered ? UITheme.BG_CANVAS : Color.WHITE;
                    fg = UITheme.PRIMARY_DARK;
                    border = isHovered ? UITheme.ACCENT_PERIWINKLE : UITheme.CARD_BORDER;
                }
                default -> { // GHOST
                    bg = isPressed ? UITheme.ACCENT_LAVENDER :
                         isHovered ? UITheme.BG_CANVAS : new Color(0, 0, 0, 0);
                    fg = UITheme.PRIMARY_DARK;
                }
            }

            // Fill background
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            // Draw border if outline
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, arc, arc));
            }

            g2.dispose();

            setForeground(fg);
            super.paintComponent(g);
        }
    }

    // ==========================================
    // 3. MODERN NAV BUTTON (SIDEBAR) WITH VECTOR ICONS
    // ==========================================
    public enum NavIconType {
        DASHBOARD, ORDERS, DRIVERS, ASSIGNMENTS, REPORTS
    }

    public static class NavButton extends JButton {
        private boolean active = false;
        private boolean isHovered = false;
        private final NavIconType iconType;
        private final String labelText;

        public NavButton(String text, NavIconType iconType) {
            super("");
            this.labelText = text;
            this.iconType = iconType;
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setForeground(UITheme.TEXT_SECONDARY);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setPreferredSize(new Dimension(200, 44));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public NavButton(String text) {
            this(text.replaceAll("[^a-zA-Z0-9 ]", "").trim(), deduceIcon(text));
        }

        private static NavIconType deduceIcon(String text) {
            String lower = text.toLowerCase();
            if (lower.contains("order")) return NavIconType.ORDERS;
            if (lower.contains("driver")) return NavIconType.DRIVERS;
            if (lower.contains("assign")) return NavIconType.ASSIGNMENTS;
            if (lower.contains("report")) return NavIconType.REPORTS;
            return NavIconType.DASHBOARD;
        }

        public void setActive(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
            setForeground(active ? UITheme.PRIMARY_DARK : UITheme.TEXT_SECONDARY);
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public String getText() {
            return labelText != null ? labelText : super.getText();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (active) {
                // Active pill matching Image 1 & Image 4
                g2.setColor(UITheme.ACCENT_LAVENDER);
                g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 12, 12));

                // Left active accent bar
                g2.setColor(UITheme.PRIMARY_DARK);
                g2.fill(new RoundRectangle2D.Float(4, 10, 4, h - 20, 4, 4));
            } else if (isHovered) {
                g2.setColor(UITheme.BG_CANVAS);
                g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 12, 12));
            }

            // Draw Vector Icon (No unicode missing glyph boxes!)
            Color iconColor = active ? UITheme.PRIMARY_DARK : UITheme.TEXT_SECONDARY;
            drawVectorIcon(g2, 18, (h - 16) / 2, iconColor);

            // Draw Text Label
            g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
            g2.setColor(active ? UITheme.PRIMARY_DARK : UITheme.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(labelText, 44, textY);

            g2.dispose();
        }

        private void drawVectorIcon(Graphics2D g2, int x, int y, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));

            switch (iconType) {
                case DASHBOARD -> {
                    // 2x2 grid squares
                    g2.fillRoundRect(x, y, 6, 6, 2, 2);
                    g2.fillRoundRect(x + 8, y, 6, 6, 2, 2);
                    g2.fillRoundRect(x, y + 8, 6, 6, 2, 2);
                    g2.fillRoundRect(x + 8, y + 8, 6, 6, 2, 2);
                }
                case ORDERS -> {
                    // Delivery Package box
                    g2.drawRoundRect(x, y + 2, 14, 12, 2, 2);
                    g2.drawLine(x, y + 6, x + 14, y + 6);
                    g2.drawLine(x + 7, y + 6, x + 7, y + 14);
                }
                case DRIVERS -> {
                    // Driver User silhouette
                    g2.drawOval(x + 3, y, 8, 8);
                    g2.drawArc(x, y + 7, 14, 10, 0, 180);
                }
                case ASSIGNMENTS -> {
                    // Transfer arrows ⇄
                    g2.drawLine(x, y + 4, x + 12, y + 4);
                    g2.drawLine(x + 9, y + 1, x + 12, y + 4);
                    g2.drawLine(x + 9, y + 7, x + 12, y + 4);

                    g2.drawLine(x + 2, y + 11, x + 14, y + 11);
                    g2.drawLine(x + 2, y + 11, x + 5, y + 8);
                    g2.drawLine(x + 2, y + 11, x + 5, y + 14);
                }
                case REPORTS -> {
                    // Ascending bar chart
                    g2.fillRect(x, y + 10, 3, 5);
                    g2.fillRect(x + 5, y + 5, 3, 10);
                    g2.fillRect(x + 10, y + 1, 3, 14);
                }
            }
        }
    }

    // ==========================================
    // 4. MODERN SCROLLBAR UI
    // ==========================================
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = UITheme.ACCENT_LAVENDER;
            this.trackColor = Color.WHITE;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(UITheme.CARD_BG);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            Color color = isThumbRollover() ? UITheme.ACCENT_PERIWINKLE : UITheme.ACCENT_LAVENDER;
            g2.setColor(color);
            int margin = 3;
            g2.fill(new RoundRectangle2D.Float(
                    thumbBounds.x + margin,
                    thumbBounds.y + margin,
                    thumbBounds.width - (margin * 2),
                    thumbBounds.height - (margin * 2),
                    8, 8
            ));
            g2.dispose();
        }
    }

    // ==========================================
    // 5. STATUS BADGE TABLE CELL RENDERER
    // ==========================================
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        private final JLabel badgeLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();

                // Draw pill background
                Color bg = getBackground();
                if (bg != null) {
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        private final JPanel container = new JPanel(new GridBagLayout());

        public StatusBadgeRenderer() {
            badgeLabel.setOpaque(false);
            badgeLabel.setFont(UITheme.FONT_BADGE);
            badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            badgeLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

            container.setOpaque(true);
            container.add(badgeLabel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            String status = value == null ? "" : value.toString().trim();
            badgeLabel.setText(status);

            Color bg;
            Color fg;

            switch (status.toLowerCase()) {
                case "delivered" -> {
                    bg = UITheme.STATUS_DELIVERED_BG;
                    fg = UITheme.STATUS_DELIVERED_FG;
                    badgeLabel.setText("● Delivered");
                }
                case "in progress" -> {
                    bg = UITheme.STATUS_PROGRESS_BG;
                    fg = UITheme.STATUS_PROGRESS_FG;
                    badgeLabel.setText("● In Progress");
                }
                case "assigned" -> {
                    bg = UITheme.STATUS_ASSIGNED_BG;
                    fg = UITheme.STATUS_ASSIGNED_FG;
                    badgeLabel.setText("● Assigned");
                }
                case "pending" -> {
                    bg = UITheme.STATUS_PENDING_BG;
                    fg = UITheme.STATUS_PENDING_FG;
                    badgeLabel.setText("● Pending");
                }
                case "failed" -> {
                    bg = UITheme.STATUS_FAILED_BG;
                    fg = UITheme.STATUS_FAILED_FG;
                    badgeLabel.setText("● Failed");
                }
                case "yes" -> {
                    bg = UITheme.STATUS_DELIVERED_BG;
                    fg = UITheme.STATUS_DELIVERED_FG;
                    badgeLabel.setText("● Available");
                }
                case "no" -> {
                    bg = new Color(240, 240, 245);
                    fg = UITheme.TEXT_MUTED;
                    badgeLabel.setText("● Busy");
                }
                default -> {
                    bg = UITheme.ACCENT_LAVENDER;
                    fg = UITheme.PRIMARY_DARK;
                    badgeLabel.setText(status);
                }
            }

            badgeLabel.setBackground(bg);
            badgeLabel.setForeground(fg);

            Integer hoveredRow = (Integer) table.getClientProperty("hoveredRow");
            if (isSelected) {
                container.setBackground(UITheme.ACCENT_LAVENDER);
            } else if (hoveredRow != null && hoveredRow == row) {
                container.setBackground(new Color(245, 242, 253));
            } else {
                container.setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 249, 255));
            }

            return container;
        }
    }

    // ==========================================
    // 6. ACTION LINK TABLE CELL RENDERER
    // ==========================================
    public static class ActionLinkRenderer extends DefaultTableCellRenderer {
        private final JLabel label = new JLabel();
        private final JPanel panel = new JPanel(new GridBagLayout());

        public ActionLinkRenderer() {
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setForeground(UITheme.PRIMARY_DARK);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            panel.setOpaque(true);
            panel.add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            label.setText(value == null ? "View Details →" : value.toString());
            Integer hoveredRow = (Integer) table.getClientProperty("hoveredRow");
            if (isSelected) {
                panel.setBackground(UITheme.ACCENT_LAVENDER);
                label.setForeground(UITheme.PRIMARY_DARK);
            } else if (hoveredRow != null && hoveredRow == row) {
                panel.setBackground(new Color(245, 242, 253));
                label.setForeground(UITheme.PRIMARY_DARK);
            } else {
                panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 249, 255));
                label.setForeground(UITheme.PRIMARY_DARK);
            }
            return panel;
        }
    }

    // ==========================================
    // 7. MODERN TEXT FIELD
    // ==========================================
    public static class ModernTextField extends JTextField {
        private final String placeholder;
        private boolean isFocused = false;

        public ModernTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(UITheme.FONT_BODY);
            setForeground(UITheme.TEXT_PRIMARY);
            setBackground(Color.WHITE);
            setCaretColor(UITheme.PRIMARY_DARK);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        public ModernTextField(int columns) {
            this("", columns);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            // Background fill
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

            // Border
            g2.setColor(isFocused ? UITheme.ACCENT_PERIWINKLE : UITheme.CARD_BORDER);
            g2.setStroke(new BasicStroke(isFocused ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, 10, 10));

            g2.dispose();
            super.paintComponent(g);

            // Placeholder
            if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
                Graphics2D gp = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(gp);
                gp.setColor(UITheme.TEXT_MUTED);
                gp.setFont(getFont());
                FontMetrics fm = gp.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                gp.drawString(placeholder, 14, y);
                gp.dispose();
            }
        }
    }

    // ==========================================
    // 8. MODERN COMBO BOX STYLING
    // ==========================================
    public static void styleComboBox(JComboBox<?> box) {
        box.setFont(UITheme.FONT_BODY);
        box.setForeground(UITheme.TEXT_PRIMARY);
        box.setBackground(Color.WHITE);
        box.setOpaque(true);
        box.setCursor(new Cursor(Cursor.HAND_CURSOR));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    // ==========================================
    // 9. MODERN SAAS TABLE ARCHITECTURE
    // ==========================================
    public static class ModernHeaderRenderer extends DefaultTableCellRenderer {
        public ModernHeaderRenderer() {
            setOpaque(true);
            setBackground(UITheme.BG_CANVAS);
            setForeground(UITheme.PRIMARY_DARK);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    public static class ModernTableCellRenderer extends DefaultTableCellRenderer {
        private final int align;

        public ModernTableCellRenderer(int align) {
            this.align = align;
            setFont(UITheme.FONT_BODY);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        }

        public ModernTableCellRenderer() {
            this(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(align);
            setForeground(UITheme.PRIMARY_DARK);

            Integer hoveredRow = (Integer) table.getClientProperty("hoveredRow");
            if (isSelected) {
                setBackground(UITheme.ACCENT_LAVENDER);
            } else if (hoveredRow != null && hoveredRow == row) {
                setBackground(new Color(245, 242, 253));
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 249, 255));
            }

            return this;
        }
    }

    public static class AvatarNameRenderer extends DefaultTableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        private final JLabel nameLabel = new JLabel();
        private final JComponent avatarBadge;
        private String initials = "";

        public AvatarNameRenderer() {
            panel.setOpaque(true);
            avatarBadge = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    int d = 26;
                    g2.setColor(UITheme.ACCENT_LAVENDER);
                    g2.fillOval(0, 0, d, d);

                    g2.setColor(UITheme.PRIMARY_DARK);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (d - fm.stringWidth(initials)) / 2;
                    int y = (d - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(initials, x, y);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(26, 26);
                }
            };

            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(UITheme.PRIMARY_DARK);

            panel.add(avatarBadge);
            panel.add(nameLabel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            String name = value == null ? "" : value.toString().trim();
            nameLabel.setText(name);

            if (name.isEmpty()) {
                initials = "";
            } else {
                String[] parts = name.split("\\s+");
                if (parts.length >= 2) {
                    initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
                } else {
                    initials = ("" + name.charAt(0)).toUpperCase();
                }
            }

            Integer hoveredRow = (Integer) table.getClientProperty("hoveredRow");
            if (isSelected) {
                panel.setBackground(UITheme.ACCENT_LAVENDER);
            } else if (hoveredRow != null && hoveredRow == row) {
                panel.setBackground(new Color(245, 242, 253));
            } else {
                panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 249, 255));
            }

            return panel;
        }
    }

    public static void styleModernTable(JTable table) {
        table.setFont(UITheme.FONT_BODY);
        table.setForeground(UITheme.PRIMARY_DARK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UITheme.ACCENT_LAVENDER);
        table.setSelectionForeground(UITheme.PRIMARY_DARK);
        table.setRowHeight(48);
        table.setGridColor(new Color(242, 238, 252));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableHeader.setBackground(UITheme.BG_CANVAS);
        tableHeader.setForeground(UITheme.PRIMARY_DARK);
        tableHeader.setPreferredSize(new Dimension(0, 46));
        tableHeader.setDefaultRenderer(new ModernHeaderRenderer());
        tableHeader.setReorderingAllowed(false);

        // Add mouse motion listener for row hover highlight
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Integer current = (Integer) table.getClientProperty("hoveredRow");
                if (current == null || current != row) {
                    table.putClientProperty("hoveredRow", row);
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("hoveredRow", -1);
                table.repaint();
            }
        });

        // Set default cell renderer with 16px padding
        table.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
    }
}
