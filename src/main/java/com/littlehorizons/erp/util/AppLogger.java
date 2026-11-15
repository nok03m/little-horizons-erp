package com.littlehorizons.erp.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Application logger that writes errors to error_log.txt in the project root.
 */
public final class AppLogger {

    private static final String LOG_FILE_NAME = "error_log.txt";
    private static final Logger LOGGER = Logger.getLogger("com.littlehorizons.erp");

    static {
        try {
            Path logPath = Path.of(LOG_FILE_NAME);
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
            FileHandler fileHandler = new FileHandler(LOG_FILE_NAME, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException exception) {
            System.err.println("Unable to initialize error_log.txt: " + exception.getMessage());
        }
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }
}
