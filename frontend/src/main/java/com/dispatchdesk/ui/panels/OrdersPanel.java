package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.AssignmentResult;
import com.dispatchdesk.model.OrderRecord;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.dialogs.DetailsDialogs;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

/**
 * Orders Management view with search/status filtering, smart dispatch, and detail popups.
 */
public class OrdersPanel extends JPanel {

    private final Frame parentFrame;
    private final IntSupplier currentUserIdSupplier;
    private final Runnable onDataChanged;
    private final Consumer<String> errorHandler;

    private final DefaultTableModel orderModel;
    private final JTable orderTable;
    private final TableRowSorter<DefaultTableModel> orderSorter;
    private final JTextField orderSearchField;
    private final JComboBox<String> orderStatusFilter;
    private final List<OrderRecord> orderRecords = new ArrayList<>();

    public OrdersPanel(Frame parentFrame, IntSupplier currentUserIdSupplier,
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
        ModernComponents.styleModernTable(orderTable);
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        orderTable.getColumnModel().getColumn(1).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(170);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(4).setCellRenderer(new ModernComponents.StatusBadgeRenderer());
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new ModernComponents.ActionLinkRenderer());

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

        orderSorter = new TableRowSorter<>(orderModel);
        orderTable.setRowSorter(orderSorter);

        orderSearchField = new ModernComponents.ModernTextField(
                "Search customer, area, or date...", 22
        );

        orderStatusFilter = new JComboBox<>(new String[]{
                "All Statuses",
                "Pending",
                "Assigned",
                "In Progress",
                "Delivered",
                "Failed"
        });
        ModernComponents.styleComboBox(orderStatusFilter);

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
        sectionTitle.setForeground(UITheme.PRIMARY_DARK);

        JLabel hint = new JLabel("Click any row or 'View Details' for address and actions");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UITheme.TEXT_SECONDARY);

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
        refresh.addActionListener(e -> onDataChanged.run());
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
        searchLabel.setForeground(UITheme.PRIMARY_DARK);

        JLabel filterLabel = new JLabel("Status:");
        filterLabel.setFont(UITheme.FONT_BODY_BOLD);
        filterLabel.setForeground(UITheme.PRIMARY_DARK);

        row2.add(searchLabel);
        row2.add(orderSearchField);
        row2.add(Box.createHorizontalStrut(12));
        row2.add(filterLabel);
        row2.add(orderStatusFilter);

        topContainer.add(row1);
        topContainer.add(row2);

        card.add(topContainer, BorderLayout.NORTH);

        orderSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyOrderFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyOrderFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyOrderFilters();
            }
        });

        orderStatusFilter.addActionListener(e -> applyOrderFilters());

        JScrollPane scroll = new JScrollPane(orderTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void loadOrders() {
        orderRecords.clear();
        orderModel.setRowCount(0);
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            List<OrderRecord> orders = DispatchBackend.getOrders(userId);
            for (OrderRecord rec : orders) {
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
            errorHandler.accept(ex.getMessage());
        }
    }

    public void resetFilters() {
        orderSearchField.setText("");
        orderStatusFilter.setSelectedIndex(0);
    }

    public void clearData() {
        orderRecords.clear();
        orderModel.setRowCount(0);
    }

    private void applyOrderFilters() {
        String searchText = orderSearchField.getText().trim();
        String selectedStatus = (String) orderStatusFilter.getSelectedItem();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!searchText.isEmpty() && !searchText.equals("Search customer, area, or date...")) {
            filters.add(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(searchText),
                    1, 2, 3
            ));
        }

        if (selectedStatus != null && !selectedStatus.equals("All Statuses")) {
            filters.add(RowFilter.regexFilter(
                    "^" + Pattern.quote(selectedStatus) + "$",
                    4
            ));
        }

        if (filters.isEmpty()) {
            orderSorter.setRowFilter(null);
        } else {
            orderSorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private void openOrderDetails(int modelRow) {
        if (modelRow < 0 || modelRow >= orderRecords.size()) return;
        OrderRecord rec = orderRecords.get(modelRow);
        DetailsDialogs.showOrderDetails(
                parentFrame,
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
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

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
                parentFrame, form, "Create New Delivery Order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        if (customer.getText().isBlank() || address.getText().isBlank() || area.getText().isBlank()) {
            errorHandler.accept("Please fill all required fields.");
            return;
        }

        try {
            LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException ex) {
            errorHandler.accept("Enter a valid date in YYYY-MM-DD format.");
            return;
        }

        try {
            DispatchBackend.createOrder(
                    userId,
                    customer.getText().trim(),
                    address.getText().trim(),
                    area.getText().trim(),
                    date.getText().trim(),
                    slot.getSelectedItem().toString()
            );
            JOptionPane.showMessageDialog(parentFrame, "Order created successfully!");
            onDataChanged.run();
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    private void assignSelectedOrder() {
        int selectedViewRow = orderTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(parentFrame, "Please select an order first.");
            return;
        }

        int selectedRow = orderTable.convertRowIndexToModel(selectedViewRow);
        if (selectedRow < 0 || selectedRow >= orderRecords.size()) return;
        OrderRecord rec = orderRecords.get(selectedRow);

        if (!rec.status.equals("Pending")) {
            JOptionPane.showMessageDialog(parentFrame, "Only pending orders can be assigned.");
            return;
        }

        assignSpecificOrder(rec.orderId, rec.area);
    }

    private void assignSpecificOrder(int orderId, String area) {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            AssignmentResult res = DispatchBackend.smartAssignOrder(userId, orderId, area);
            if (!res.success) {
                JOptionPane.showMessageDialog(parentFrame, res.message);
                return;
            }

            JOptionPane.showMessageDialog(
                    parentFrame,
                    "Driver assigned successfully!\n\n" +
                            "Driver: " + res.driverName +
                            "\nOrder ID: " + orderId +
                            "\nArea: " + area
            );
            onDataChanged.run();
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    private void editSelectedOrder() {
        int selectedViewRow = orderTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(parentFrame, "Please select an order to edit.");
            return;
        }

        int selectedRow = orderTable.convertRowIndexToModel(selectedViewRow);
        if (selectedRow < 0 || selectedRow >= orderRecords.size()) return;
        editSpecificOrder(orderRecords.get(selectedRow));
    }

    private void editSpecificOrder(OrderRecord rec) {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        if (!rec.status.equals("Pending")) {
            JOptionPane.showMessageDialog(parentFrame, "Only pending orders can be edited.");
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
                parentFrame,
                form,
                "Edit Order #" + rec.orderId,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        String newCustomer = customerField.getText().trim();
        String newAddress = addressField.getText().trim();
        String newArea = areaField.getText().trim();
        String newDate = dateField.getText().trim();
        String newTime = timeField.getText().trim();

        if (newCustomer.isEmpty() || newAddress.isEmpty() || newArea.isEmpty()
                || newDate.isEmpty() || newTime.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "Please fill in all fields.");
            return;
        }

        try {
            boolean updated = DispatchBackend.updatePendingOrder(
                    userId, rec.orderId, newCustomer, newAddress, newArea, newDate, newTime
            );

            if (updated) {
                JOptionPane.showMessageDialog(parentFrame, "Order updated successfully!");
                onDataChanged.run();
            } else {
                JOptionPane.showMessageDialog(parentFrame, "Order could not be updated.");
            }
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }
}
