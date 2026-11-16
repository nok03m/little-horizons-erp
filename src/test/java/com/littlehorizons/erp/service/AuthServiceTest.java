package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.ApplicationRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    void passwordHashVerifiesOnlyTheOriginalPassword() {
        char[] password = "Correct-Horse-2026".toCharArray();
        String hash = AuthService.hashPassword(password);

        assertTrue(AuthService.verifyPassword(password, hash));
        assertFalse(AuthService.verifyPassword("incorrect-password".toCharArray(), hash));
        assertFalse(AuthService.verifyPassword(password, "not-a-valid-hash"));
    }

    @Test
    void passwordHashesUseIndependentSalts() {
        char[] password = "Correct-Horse-2026".toCharArray();

        assertNotEquals(AuthService.hashPassword(password), AuthService.hashPassword(password));
    }

    @Test
    void rolesExposeOnlyTheirConfiguredTopLevelModules() {
        assertTrue(ApplicationRole.FINANCE.canAccessMenuOption(5));
        assertFalse(ApplicationRole.FINANCE.canAccessMenuOption(1));
        assertTrue(ApplicationRole.LIBRARIAN.canAccessMenuOption(6));
        assertFalse(ApplicationRole.LIBRARIAN.canAccessMenuOption(5));
        assertTrue(ApplicationRole.ADMIN.canAccessMenuOption(7));
    }
}
