package com.dispatchdesk;

import com.dispatchdesk.api.BackendApiServer;
import com.dispatchdesk.config.Database;

import java.util.concurrent.CountDownLatch;

/**
 * Standalone entry point for the DispatchDesk Java Backend REST API Server.
 * Initializes the SQLite database and starts the HTTP server on PORT (default 8080).
 */
public class BackendMain {
    public static void main(String[] args) throws InterruptedException {
        int port = resolvePort(args);

        Database.initialize();
        boolean started = BackendApiServer.start(port);
        if (!started) {
            System.err.println("Failed to start DispatchDesk Backend API Server on port " + port);
            System.exit(1);
        }

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down DispatchDesk Backend API Server...");
            BackendApiServer.stop();
            shutdownLatch.countDown();
        }, "DispatchDesk-Shutdown"));

        System.out.println("DispatchDesk Java Backend is running and ready for requests.");
        shutdownLatch.await();
    }

    private static int resolvePort(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }
}
