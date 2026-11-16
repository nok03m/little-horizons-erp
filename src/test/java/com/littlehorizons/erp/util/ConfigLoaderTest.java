package com.littlehorizons.erp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {

    @Test
    void allowsDisabledTlsOnlyForLoopback() {
        assertDoesNotThrow(() -> ConfigLoader.validateTlsMode("127.0.0.1", "sslMode=DISABLED"));
        assertDoesNotThrow(() -> ConfigLoader.validateTlsMode("localhost", "useSSL=false"));
    }

    @Test
    void requiresIdentityVerificationForRemoteDatabase() {
        assertDoesNotThrow(() -> ConfigLoader.validateTlsMode("db.example.internal", "sslMode=VERIFY_IDENTITY"));
        assertThrows(IllegalStateException.class,
                () -> ConfigLoader.validateTlsMode("db.example.internal", "sslMode=DISABLED"));
        assertThrows(IllegalStateException.class,
                () -> ConfigLoader.validateTlsMode("db.example.internal", "serverTimezone=UTC"));
    }
}
