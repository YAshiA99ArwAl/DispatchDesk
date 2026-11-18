package com.dispatchdesk;

import com.dispatchdesk.api.BackendApiServer;
import com.dispatchdesk.db.Database;
import com.dispatchdesk.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point for DispatchDesk.
 * Initializes SQLite persistence, starts the built-in REST API server, and launches the desktop UI.
 */
public class Main {
    private static final int DEFAULT_API_PORT = 8080;

    public static void main(String[] args) {
        Database.initialize();
        BackendApiServer.start(DEFAULT_API_PORT);

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
