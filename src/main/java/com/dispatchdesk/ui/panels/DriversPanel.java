package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.DriverRecord;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Drivers Directory view for managing fleet drivers and toggling availability.
 */
public class DriversPanel extends JPanel {

    private final Frame parentFrame;
    private final IntSupplier currentUserIdSupplier;
    private final Runnable onDataChanged;
    private final Consumer<String> errorHandler;

    private final DefaultTableModel driverModel;

    public DriversPanel(Frame parentFrame, IntSupplier currentUserIdSupplier,
                        Runnable onDataChanged, Consumer<String> errorHandler) {
        super(new BorderLayout(16, 16));
        this.parentFrame = parentFrame;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.onDataChanged = onDataChanged;
        this.errorHandler = errorHandler;

        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

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
        ModernComponents.styleModernTable(table);
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
        sectionTitle.setForeground(UITheme.PRIMARY_DARK);

        JLabel hint = new JLabel("Click any row or 'Toggle Status' to update availability");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UITheme.TEXT_SECONDARY);

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
        refresh.addActionListener(e -> onDataChanged.run());
        availability.addActionListener(e -> toggleDriverAvailability(table));

        buttons.add(add);
        buttons.add(availability);
        buttons.add(refresh);

        toolbar.add(titleBlock, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    public void loadDrivers() {
        driverModel.setRowCount(0);
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            List<DriverRecord> drivers = DispatchBackend.getDrivers(userId);
            for (DriverRecord d : drivers) {
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
            errorHandler.accept(ex.getMessage());
        }
    }

    public void clearData() {
        driverModel.setRowCount(0);
    }

    private void addDriver() {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

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
                parentFrame, form, "Add New Fleet Driver", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().isBlank() || phone.getText().isBlank() || area.getText().isBlank()) {
            errorHandler.accept("Please fill all fields.");
            return;
        }

        try {
            DispatchBackend.addDriver(
                    userId,
                    name.getText().trim(),
                    phone.getText().trim(),
                    area.getText().trim()
            );
            JOptionPane.showMessageDialog(parentFrame, "Driver added successfully!");
            onDataChanged.run();
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    private void toggleDriverAvailability(JTable table) {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(parentFrame, "Please select a driver first.");
            return;
        }

        String rawId = table.getValueAt(selectedRow, 0).toString().replace("#", "").trim();
        int driverId = Integer.parseInt(rawId);

        String currentAvailability = table.getValueAt(selectedRow, 4).toString();
        boolean newAvailability =
                !(currentAvailability.equalsIgnoreCase("Yes") || currentAvailability.contains("Available"));

        try {
            boolean updated = DispatchBackend.updateDriverAvailability(userId, driverId, newAvailability);
            if (updated) {
                String message = newAvailability
                        ? "Driver is now Available."
                        : "Driver is now Unavailable.";
                JOptionPane.showMessageDialog(parentFrame, message);
                onDataChanged.run();
            } else {
                errorHandler.accept("Driver could not be updated.");
            }
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }
}
