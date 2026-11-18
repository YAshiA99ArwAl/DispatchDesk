package com.dispatchdesk.ui.panels;

import com.dispatchdesk.model.UserSession;
import com.dispatchdesk.service.DispatchBackend;
import com.dispatchdesk.ui.components.ModernComponents;
import com.dispatchdesk.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Modern, professional login & account creation panel for DispatchDesk.
 * Provides pixel-perfect alignment and full support for signing in and creating new IDs.
 */
public class LoginPanel extends JPanel {

    public enum Mode {
        SIGN_IN, CREATE_ID
    }

    private Mode currentMode = Mode.SIGN_IN;
    private final Consumer<UserSession> onLoginSuccess;

    // Sign In Fields
    private final ModernComponents.ModernTextField signInUserField;
    private final JPasswordField signInPassField;
    private final JButton signInShowHideBtn;

    // Create ID Fields
    private final ModernComponents.ModernTextField regFullNameField;
    private final ModernComponents.ModernTextField regUsernameField;
    private final JPasswordField regPassField;
    private final JButton regShowHideBtn;
    private final JPasswordField regConfirmPassField;

    // Shared UI Containers
    private final ModernComponents.RoundedCard card;
    private final JPanel segmentedSwitch;
    private final JButton tabSignIn;
    private final JButton tabCreateId;
    private final JLabel welcomeMsg;
    private final JPanel errorBox;
    private final JLabel errorLabel;
    private final JLabel errIcon;
    private final CardLayout formCardLayout;
    private final JPanel formCardContainer;

    private static final int FORM_WIDTH = 370;

    public LoginPanel(Consumer<UserSession> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        setBackground(UITheme.BG_CANVAS);

        // Center Login Card
        card = new ModernComponents.RoundedCard(20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        card.setPreferredSize(new Dimension(440, 600));
        card.setMaximumSize(new Dimension(440, 600));

        // 1. Logo Badge & Branding Header
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComponent logoBadge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.PRIMARY_DARK);
                g2.fill(new RoundRectangle2D.Float(0, 0, 46, 46, 12, 12));

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                int x = (46 - fm.stringWidth("D")) / 2;
                int y = (46 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("D", x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(46, 46);
            }
        };
        logoPanel.add(logoBadge);

        JLabel brandTitle = new JLabel("DispatchDesk");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brandTitle.setForeground(UITheme.PRIMARY_DARK);
        brandTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel portalSubtitle = new JLabel("COORDINATOR PORTAL");
        portalSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        portalSubtitle.setForeground(UITheme.ACCENT_PERIWINKLE.darker());
        portalSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomeMsg = new JLabel("Sign in to access your dispatch operations");
        welcomeMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        welcomeMsg.setForeground(UITheme.TEXT_SECONDARY);
        welcomeMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Segmented Mode Switcher (Sign In vs Create ID)
        segmentedSwitch = new JPanel(new GridLayout(1, 2, 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.BG_CANVAS);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(UITheme.CARD_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        segmentedSwitch.setOpaque(false);
        segmentedSwitch.setPreferredSize(new Dimension(FORM_WIDTH, 38));
        segmentedSwitch.setMaximumSize(new Dimension(FORM_WIDTH, 38));
        segmentedSwitch.setAlignmentX(Component.CENTER_ALIGNMENT);
        segmentedSwitch.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        tabSignIn = createSegmentTab("Sign In", true);
        tabCreateId = createSegmentTab("Create ID", false);

        tabSignIn.addActionListener(e -> setMode(Mode.SIGN_IN));
        tabCreateId.addActionListener(e -> setMode(Mode.CREATE_ID));

        segmentedSwitch.add(tabSignIn);
        segmentedSwitch.add(tabCreateId);

        // 3. Status/Error Alert Box
        errorBox = new JPanel(new BorderLayout(8, 0));
        errorBox.setBackground(UITheme.STATUS_FAILED_BG);
        errorBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(248, 180, 180), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        errorBox.setPreferredSize(new Dimension(FORM_WIDTH, 34));
        errorBox.setMaximumSize(new Dimension(FORM_WIDTH, 34));
        errorBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorBox.setVisible(false);

        errIcon = new JLabel("⚠");
        errIcon.setForeground(UITheme.STATUS_FAILED_FG);
        errIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));

        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        errorLabel.setForeground(UITheme.STATUS_FAILED_FG);

        errorBox.add(errIcon, BorderLayout.WEST);
        errorBox.add(errorLabel, BorderLayout.CENTER);

        // 4. Form Cards (CardLayout: SIGN_IN vs CREATE_ID)
        formCardLayout = new CardLayout();
        formCardContainer = new JPanel(formCardLayout);
        formCardContainer.setOpaque(false);
        formCardContainer.setPreferredSize(new Dimension(FORM_WIDTH, 320));
        formCardContainer.setMaximumSize(new Dimension(FORM_WIDTH, 320));
        formCardContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ==========================================
        // SUB-PANEL A: SIGN IN FORM
        // ==========================================
        JPanel signInPanel = new JPanel();
        signInPanel.setLayout(new BoxLayout(signInPanel, BoxLayout.Y_AXIS));
        signInPanel.setOpaque(false);

        JLabel userLabel = createFormLabel("Username or Email");
        signInUserField = new ModernComponents.ModernTextField("Enter username (e.g. admin)", 20);
        styleFieldSize(signInUserField, 42);

        JLabel passLabel = createFormLabel("Password");
        signInPassField = createModernPasswordField("Enter password (e.g. admin123)");
        signInShowHideBtn = createModernShowHideButton(signInPassField);

        JPanel signInPassRow = new JPanel(new BorderLayout(8, 0));
        signInPassRow.setOpaque(false);
        styleFieldSize(signInPassRow, 42);
        signInPassRow.add(signInPassField, BorderLayout.CENTER);
        signInPassRow.add(signInShowHideBtn, BorderLayout.EAST);

        ModernComponents.ModernButton signInBtn = new ModernComponents.ModernButton(
                "Sign In to DispatchDesk →", ModernComponents.ButtonStyle.PRIMARY
        );
        styleFieldSize(signInBtn, 44);
        signInBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        signInBtn.addActionListener(e -> attemptSignIn());

        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        demoPanel.setOpaque(false);
        styleFieldSize(demoPanel, 28);
        JButton demoBtn = new JButton("Demo Login: admin / admin123 (Click to fill)");
        demoBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        demoBtn.setForeground(UITheme.PRIMARY_DARK);
        demoBtn.setBackground(UITheme.ACCENT_LAVENDER);
        demoBtn.setFocusPainted(false);
        demoBtn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        demoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        demoBtn.addActionListener(e -> {
            signInUserField.setText("admin");
            signInPassField.setText("admin123");
            hideAlert();
        });
        demoPanel.add(demoBtn);

        JPanel switchLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchLinkPanel.setOpaque(false);
        styleFieldSize(switchLinkPanel, 24);
        JLabel noAccountLabel = new JLabel("Don't have an ID?");
        noAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noAccountLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton createLink = new JButton("Create an ID here");
        createLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createLink.setForeground(UITheme.PRIMARY_DARK);
        createLink.setBorder(BorderFactory.createEmptyBorder());
        createLink.setContentAreaFilled(false);
        createLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createLink.addActionListener(e -> setMode(Mode.CREATE_ID));
        switchLinkPanel.add(noAccountLabel);
        switchLinkPanel.add(createLink);

        signInPanel.add(userLabel);
        signInPanel.add(Box.createVerticalStrut(4));
        signInPanel.add(signInUserField);
        signInPanel.add(Box.createVerticalStrut(10));
        signInPanel.add(passLabel);
        signInPanel.add(Box.createVerticalStrut(4));
        signInPanel.add(signInPassRow);
        signInPanel.add(Box.createVerticalStrut(18));
        signInPanel.add(signInBtn);
        signInPanel.add(Box.createVerticalStrut(12));
        signInPanel.add(demoPanel);
        signInPanel.add(Box.createVerticalStrut(8));
        signInPanel.add(switchLinkPanel);

        // ==========================================
        // SUB-PANEL B: CREATE ID FORM
        // ==========================================
        JPanel createIdPanel = new JPanel();
        createIdPanel.setLayout(new BoxLayout(createIdPanel, BoxLayout.Y_AXIS));
        createIdPanel.setOpaque(false);

        JLabel regNameLabel = createFormLabel("Full Name");
        regFullNameField = new ModernComponents.ModernTextField("e.g. Alex Morgan", 20);
        styleFieldSize(regFullNameField, 38);

        JLabel regUserLabel = createFormLabel("Choose Coordinator ID / Username");
        regUsernameField = new ModernComponents.ModernTextField("e.g. alex", 20);
        styleFieldSize(regUsernameField, 38);

        JLabel regPassLabel = createFormLabel("Create Password");
        regPassField = createModernPasswordField("Choose a password");
        regShowHideBtn = createModernShowHideButton(regPassField);

        JPanel regPassRow = new JPanel(new BorderLayout(8, 0));
        regPassRow.setOpaque(false);
        styleFieldSize(regPassRow, 38);
        regPassRow.add(regPassField, BorderLayout.CENTER);
        regPassRow.add(regShowHideBtn, BorderLayout.EAST);

        JLabel regConfirmLabel = createFormLabel("Confirm Password");
        regConfirmPassField = createModernPasswordField("Re-enter password");
        styleFieldSize(regConfirmPassField, 38);

        ModernComponents.ModernButton createIdBtn = new ModernComponents.ModernButton(
                "Create ID & Sign In →", ModernComponents.ButtonStyle.PRIMARY
        );
        styleFieldSize(createIdBtn, 42);
        createIdBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        createIdBtn.addActionListener(e -> attemptCreateId());

        JPanel backToLoginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        backToLoginPanel.setOpaque(false);
        styleFieldSize(backToLoginPanel, 24);
        JLabel haveAccountLabel = new JLabel("Already have an ID?");
        haveAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        haveAccountLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton loginLink = new JButton("Sign In here");
        loginLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loginLink.setForeground(UITheme.PRIMARY_DARK);
        loginLink.setBorder(BorderFactory.createEmptyBorder());
        loginLink.setContentAreaFilled(false);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addActionListener(e -> setMode(Mode.SIGN_IN));
        backToLoginPanel.add(haveAccountLabel);
        backToLoginPanel.add(loginLink);

        createIdPanel.add(regNameLabel);
        createIdPanel.add(Box.createVerticalStrut(2));
        createIdPanel.add(regFullNameField);
        createIdPanel.add(Box.createVerticalStrut(8));
        createIdPanel.add(regUserLabel);
        createIdPanel.add(Box.createVerticalStrut(2));
        createIdPanel.add(regUsernameField);
        createIdPanel.add(Box.createVerticalStrut(8));
        createIdPanel.add(regPassLabel);
        createIdPanel.add(Box.createVerticalStrut(2));
        createIdPanel.add(regPassRow);
        createIdPanel.add(Box.createVerticalStrut(8));
        createIdPanel.add(regConfirmLabel);
        createIdPanel.add(Box.createVerticalStrut(2));
        createIdPanel.add(regConfirmPassField);
        createIdPanel.add(Box.createVerticalStrut(14));
        createIdPanel.add(createIdBtn);
        createIdPanel.add(Box.createVerticalStrut(8));
        createIdPanel.add(backToLoginPanel);

        formCardContainer.add(signInPanel, "SIGN_IN");
        formCardContainer.add(createIdPanel, "CREATE_ID");

        KeyAdapter enterSignIn = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptSignIn();
                }
            }
        };
        signInUserField.addKeyListener(enterSignIn);
        signInPassField.addKeyListener(enterSignIn);

        KeyAdapter enterCreate = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptCreateId();
                }
            }
        };
        regFullNameField.addKeyListener(enterCreate);
        regUsernameField.addKeyListener(enterCreate);
        regPassField.addKeyListener(enterCreate);
        regConfirmPassField.addKeyListener(enterCreate);

        card.add(logoPanel);
        card.add(Box.createVerticalStrut(6));
        card.add(brandTitle);
        card.add(Box.createVerticalStrut(2));
        card.add(portalSubtitle);
        card.add(Box.createVerticalStrut(4));
        card.add(welcomeMsg);
        card.add(Box.createVerticalStrut(14));

        card.add(segmentedSwitch);
        card.add(Box.createVerticalStrut(12));

        card.add(errorBox);
        card.add(Box.createVerticalStrut(10));

        card.add(formCardContainer);

        add(card);
    }

    private JButton createSegmentTab(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                boolean isCurrent = (currentMode == Mode.SIGN_IN && "Sign In".equals(getText()))
                        || (currentMode == Mode.CREATE_ID && "Create ID".equals(getText()));

                if (isCurrent) {
                    g2.setColor(Color.WHITE);
                    g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 9, 9));
                    g2.setColor(UITheme.CARD_BORDER);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 9, 9));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(active ? UITheme.PRIMARY_DARK : UITheme.TEXT_SECONDARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        hideAlert();

        if (mode == Mode.SIGN_IN) {
            formCardLayout.show(formCardContainer, "SIGN_IN");
            welcomeMsg.setText("Sign in to access your dispatch operations");
            card.setPreferredSize(new Dimension(440, 560));
            card.setMaximumSize(new Dimension(440, 560));
            formCardContainer.setPreferredSize(new Dimension(FORM_WIDTH, 270));
            formCardContainer.setMaximumSize(new Dimension(FORM_WIDTH, 270));
            tabSignIn.setForeground(UITheme.PRIMARY_DARK);
            tabCreateId.setForeground(UITheme.TEXT_SECONDARY);
            SwingUtilities.invokeLater(signInUserField::requestFocusInWindow);
        } else {
            formCardLayout.show(formCardContainer, "CREATE_ID");
            welcomeMsg.setText("Register a new coordinator ID to access the console");
            card.setPreferredSize(new Dimension(440, 620));
            card.setMaximumSize(new Dimension(440, 620));
            formCardContainer.setPreferredSize(new Dimension(FORM_WIDTH, 340));
            formCardContainer.setMaximumSize(new Dimension(FORM_WIDTH, 340));
            tabCreateId.setForeground(UITheme.PRIMARY_DARK);
            tabSignIn.setForeground(UITheme.TEXT_SECONDARY);
            SwingUtilities.invokeLater(regFullNameField::requestFocusInWindow);
        }

        segmentedSwitch.repaint();
        card.revalidate();
        card.repaint();
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.PRIMARY_DARK);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setPreferredSize(new Dimension(FORM_WIDTH, 18));
        lbl.setMaximumSize(new Dimension(FORM_WIDTH, 18));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        return lbl;
    }

    private void styleFieldSize(JComponent comp, int height) {
        comp.setPreferredSize(new Dimension(FORM_WIDTH, height));
        comp.setMaximumSize(new Dimension(FORM_WIDTH, height));
        comp.setMinimumSize(new Dimension(FORM_WIDTH, height));
        comp.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private JPasswordField createModernPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                int w = getWidth() - 1;
                int h = getHeight() - 1;

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

                boolean focused = hasFocus();
                g2.setColor(focused ? UITheme.ACCENT_PERIWINKLE : UITheme.CARD_BORDER);
                g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, 10, 10));
                g2.dispose();

                super.paintComponent(g);

                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D gp = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(gp);
                    gp.setColor(UITheme.TEXT_MUTED);
                    gp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    FontMetrics fm = gp.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    gp.drawString(placeholder, 14, y);
                    gp.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(UITheme.TEXT_PRIMARY);
        field.setCaretColor(UITheme.PRIMARY_DARK);
        field.setEchoChar('•');
        field.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.repaint();
            }
        });
        return field;
    }

    private JButton createModernShowHideButton(JPasswordField targetField) {
        JButton btn = new JButton("Show") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                int w = getWidth() - 1;
                int h = getHeight() - 1;

                g2.setColor(getModel().isRollover() ? UITheme.ACCENT_LAVENDER : UITheme.BG_CANVAS);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

                g2.setColor(UITheme.CARD_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(64, 42));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(UITheme.PRIMARY_DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if ("Show".equals(btn.getText())) {
                targetField.setEchoChar((char) 0);
                btn.setText("Hide");
            } else {
                targetField.setEchoChar('•');
                btn.setText("Show");
            }
            targetField.repaint();
        });
        return btn;
    }

    private void attemptSignIn() {
        String username = signInUserField.getText().trim();
        String password = new String(signInPassField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Please enter your username and password.", false);
            if (username.isEmpty()) signInUserField.requestFocusInWindow();
            else signInPassField.requestFocusInWindow();
            return;
        }

        UserSession session = DispatchBackend.login(username, password);
        if (session != null) {
            hideAlert();
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(session);
            }
        } else {
            showAlert("Invalid username or password. Please try again.", false);
            signInPassField.setText("");
            signInPassField.requestFocusInWindow();
        }
    }

    private void attemptCreateId() {
        String fullName = regFullNameField.getText().trim();
        String username = regUsernameField.getText().trim();
        String password = new String(regPassField.getPassword()).trim();
        String confirm = new String(regConfirmPassField.getPassword()).trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert("Please fill in all registration fields.", false);
            return;
        }

        if (username.length() < 3) {
            showAlert("Username/ID must be at least 3 characters.", false);
            regUsernameField.requestFocusInWindow();
            return;
        }

        if (password.length() < 4) {
            showAlert("Password must be at least 4 characters.", false);
            regPassField.requestFocusInWindow();
            return;
        }

        if (!password.equals(confirm)) {
            showAlert("Passwords do not match. Please verify.", false);
            regConfirmPassField.setText("");
            regConfirmPassField.requestFocusInWindow();
            return;
        }

        if (DispatchBackend.usernameExists(username)) {
            showAlert("Username '" + username + "' is already taken. Please pick another.", false);
            regUsernameField.requestFocusInWindow();
            return;
        }

        UserSession newSession = DispatchBackend.register(username, password, fullName, "Coordinator");
        if (newSession != null) {
            hideAlert();
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(newSession);
            }
        } else {
            showAlert("Could not register user. Please try again.", false);
        }
    }

    public void showAlert(String message, boolean isSuccess) {
        errorLabel.setText(message);
        if (isSuccess) {
            errorBox.setBackground(UITheme.STATUS_DELIVERED_BG);
            errorBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 230, 195), 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            errIcon.setText("✓");
            errIcon.setForeground(UITheme.STATUS_DELIVERED_FG);
            errorLabel.setForeground(UITheme.STATUS_DELIVERED_FG);
        } else {
            errorBox.setBackground(UITheme.STATUS_FAILED_BG);
            errorBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(248, 180, 180), 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            errIcon.setText("⚠");
            errIcon.setForeground(UITheme.STATUS_FAILED_FG);
            errorLabel.setForeground(UITheme.STATUS_FAILED_FG);
        }
        errorBox.setVisible(true);
        revalidate();
        repaint();
    }

    public void hideAlert() {
        errorBox.setVisible(false);
        errorLabel.setText("");
        revalidate();
        repaint();
    }

    public void resetFields() {
        signInUserField.setText("");
        signInPassField.setText("");
        signInShowHideBtn.setText("Show");
        signInPassField.setEchoChar('•');

        regFullNameField.setText("");
        regUsernameField.setText("");
        regPassField.setText("");
        regConfirmPassField.setText("");
        regShowHideBtn.setText("Show");
        regPassField.setEchoChar('•');

        setMode(Mode.SIGN_IN);
    }

    public void focusInitial() {
        SwingUtilities.invokeLater(signInUserField::requestFocusInWindow);
    }
}
