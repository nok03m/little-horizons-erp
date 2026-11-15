package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database configuration from application.properties.
 * Environment variables override file values when present.
 */
public final class ConfigLoader {

    private static final String PROPERTIES_FILE = "application.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing classpath resource: " + PROPERTIES_FILE);
            }
            PROPERTIES.load(input);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private ConfigLoader() {
    }

    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return PROPERTIES.getProperty(key);
    }

    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required configuration missing: " + key);
        }
        return value;
    }

    public static String buildJdbcUrl() {
        String host = getRequired("db.host");
        String port = getRequired("db.port");
        String name = getRequired("db.name");
        String params = get("db.params");
        validateTlsMode(host, params);
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(host).append(':').append(port).append('/').append(name);
        if (params != null && !params.isBlank()) {
            url.append('?').append(params);
        }
        return url.toString();
    }

    static void validateTlsMode(String host, String params) {
        if (isLoopback(host)) return;

        boolean verifiesIdentity = false;
        boolean disablesTls = false;
        if (params != null) {
            for (String item : params.split("&")) {
                String[] pair = item.split("=", 2);
                String key = pair[0].trim().toLowerCase();
                String value = pair.length == 2 ? pair[1].trim().toLowerCase() : "";
                if ("sslmode".equals(key) && "verify_identity".equals(value)) verifiesIdentity = true;
                if (("sslmode".equals(key) && "disabled".equals(value))
                        || ("usessl".equals(key) && "false".equals(value))
                        || ("requiressl".equals(key) && "false".equals(value))) disablesTls = true;
            }
        }
        if (!verifiesIdentity || disablesTls) {
            throw new IllegalStateException("Remote MySQL connections require sslMode=VERIFY_IDENTITY.");
        }
    }

    private static boolean isLoopback(String host) {
        String normalized = host.trim().toLowerCase();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return "localhost".equals(normalized) || "::1".equals(normalized)
                || normalized.matches("127(?:\\.[0-9]{1,3}){3}");
    }
}
