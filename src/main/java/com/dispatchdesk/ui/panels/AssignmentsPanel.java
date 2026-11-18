package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.AssignmentRecord;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.dialogs.DetailsDialogs;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Delivery Assignments view for tracking driver assignments and updating delivery lifecycle status.
 */
public class AssignmentsPanel extends JPanel {

    private final Frame parentFrame;
    private final IntSupplier currentUserIdSupplier;
    private final Runnable onDataChanged;
    private final Consumer<String> errorHandler;

    private final DefaultTableModel assignmentModel;
    private final List<AssignmentRecord> assignmentRecords = new ArrayList<>();

    public AssignmentsPanel(Frame parentFrame, IntSupplier currentUserIdSupplier,
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

        assignmentModel = new DefaultTableModel(
                new String[]{
                        "Order ID", "Customer", "Assigned Driver",
                        "Delivery Area", "Status", "Quick Action"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(assignmentModel);
        ModernComponents.styleModernTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setCellRenderer(new ModernComponents.AvatarNameRenderer());
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setCellRenderer(new ModernComponents.StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setCellRenderer(new ModernComponents.ActionLinkRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (e.getClickCount() == 2 || viewCol == 5) {
                        openAssignmentDetails(modelRow);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);

        // Header Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel sectionTitle = new JLabel("Driver Assignments & Status");
        sectionTitle.setFont(UITheme.FONT_SECTION);
        sectionTitle.setForeground(UITheme.PRIMARY_DARK);

        JLabel hint = new JLabel("Click any row or 'View Details' for dispatch info");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UITheme.TEXT_SECONDARY);

        titleBlock.add(sectionTitle);
        titleBlock.add(hint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        ModernComponents.ModernButton update = new ModernComponents.ModernButton(
                "Update Delivery Status", ModernComponents.ButtonStyle.PRIMARY
        );
        ModernComponents.ModernButton refresh = new ModernComponents.ModernButton(
                "Refresh Assignments", ModernComponents.ButtonStyle.OUTLINE
        );

        refresh.addActionListener(e -> loadAssignments());
        update.addActionListener(e -> updateDeliveryStatus(table));

        buttons.add(update);
        buttons.add(refresh);

        toolbar.add(titleBlock, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    public void loadAssignments() {
        assignmentRecords.clear();
        assignmentModel.setRowCount(0);
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        try {
            List<AssignmentRecord> rows = DispatchBackend.getAssignments(userId);
            for (AssignmentRecord rec : rows) {
                assignmentRecords.add(rec);
                assignmentModel.addRow(new Object[]{
                        "#" + rec.orderId,
                        rec.customer,
                        rec.driver,
                        rec.area,
                        rec.status,
                        "View Details →"
                });
            }
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    public void clearData() {
        assignmentRecords.clear();
        assignmentModel.setRowCount(0);
    }

    private void openAssignmentDetails(int modelRow) {
        if (modelRow < 0 || modelRow >= assignmentRecords.size()) return;
        AssignmentRecord rec = assignmentRecords.get(modelRow);
        DetailsDialogs.showAssignmentDetails(
                parentFrame,
                rec.orderId,
                rec.customer,
                rec.driver,
                rec.area,
                rec.status,
                rec.assignedAt,
                rec.failureReason,
                () -> updateSingleAssignmentStatus(rec.orderId, rec.status)
        );
    }

    private void updateDeliveryStatus(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(parentFrame, "Please select an assignment first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow >= 0 && modelRow < assignmentRecords.size()) {
            AssignmentRecord rec = assignmentRecords.get(modelRow);
            updateSingleAssignmentStatus(rec.orderId, rec.status);
        }
    }

    private void updateSingleAssignmentStatus(int orderId, String currentStatus) {
        int userId = currentUserIdSupplier.getAsInt();
        if (userId <= 0) return;

        if (currentStatus.equals("Delivered") || currentStatus.equals("Failed")) {
            JOptionPane.showMessageDialog(parentFrame, "This delivery is already completed.");
            return;
        }

        String[] statuses = {"In Progress", "Delivered", "Failed"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        ModernComponents.styleComboBox(statusBox);

        JPanel panel = new JPanel(new GridLayout(2, 1, 6, 8));
        panel.add(new JLabel("Select updated delivery status:"));
        panel.add(statusBox);

        int result = JOptionPane.showConfirmDialog(
                parentFrame, panel, "Update Status - Order #" + orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return;

        String newStatus = statusBox.getSelectedItem().toString();
        String failureReason = null;

        if (newStatus.equals("Failed")) {
            failureReason = JOptionPane.showInputDialog(
                    parentFrame, "Please enter the reason for delivery failure:",
                    "Delivery Failure Reason", JOptionPane.QUESTION_MESSAGE
            );
            if (failureReason == null) return;
            failureReason = failureReason.trim();
            if (failureReason.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame, "Failure reason cannot be empty.");
                return;
            }
        }

        try {
            boolean updated = DispatchBackend.updateDeliveryStatus(userId, orderId, newStatus, failureReason);
            if (updated) {
                JOptionPane.showMessageDialog(parentFrame, "Delivery status updated to " + newStatus);
                onDataChanged.run();
            } else {
                errorHandler.accept("Order could not be updated.");
            }
        } catch (SQLException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }
}
