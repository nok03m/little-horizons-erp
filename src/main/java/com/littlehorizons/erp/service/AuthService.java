package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.UserAccountDao;
import com.littlehorizons.erp.model.ApplicationRole;
import com.littlehorizons.erp.model.UserAccount;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Console;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/** Console authentication using salted PBKDF2 password hashes. */
public final class AuthService {

    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountDao userAccountDao;

    public AuthService(UserAccountDao userAccountDao) {
        this.userAccountDao = userAccountDao;
    }

    public Optional<UserAccount> login(Console console) {
        return authenticate(console, null);
    }

    public boolean bootstrapAdministrator(Console console) {
        console.printf("Create the first ERP administrator account.%n");
        String username = readUsername(console);
        char[] password = readConfirmedPassword(console);
        try {
            if (!userAccountDao.insertFirstAdministrator(username, hashPassword(password))) {
                console.printf("Accounts already exist. Bootstrap is disabled.%n");
                return false;
            }
            console.printf("Administrator account created.%n");
            return true;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public boolean createAccountAsAdministrator(Console console) {
        if (authenticate(console, ApplicationRole.ADMIN).isEmpty()) {
            return false;
        }

        String username = readUsername(console);
        ApplicationRole role = readRole(console);
        char[] password = readConfirmedPassword(console);
        try {
            userAccountDao.insert(username, hashPassword(password), role);
            console.printf("Account created with role %s.%n", role);
            return true;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public boolean setAccountActiveAsAdministrator(Console console, String username, boolean active) {
        Optional<UserAccount> administrator = authenticate(console, ApplicationRole.ADMIN);
        if (administrator.isEmpty()) return false;
        if (!active && administrator.get().username().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("An administrator cannot disable their own account.");
        }
        return userAccountDao.setActive(username, active);
    }

    public static String hashPassword(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password, salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(char[] password, String encodedHash) {
        try {
            String[] parts = encodedHash.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = deriveKey(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Optional<UserAccount> authenticate(Console console, ApplicationRole requiredRole) {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = console.readLine("Username: ").trim();
            char[] password = console.readPassword("Password: ");
            if (password == null) {
                return Optional.empty();
            }
            try {
                Optional<UserAccountDao.StoredUserAccount> stored = userAccountDao.findActiveByUsername(username);
                boolean locked = stored.map(UserAccountDao.StoredUserAccount::locked).orElse(false);
                if (stored.isPresent() && !locked
                        && verifyPassword(password, stored.get().passwordHash())
                        && (requiredRole == null || stored.get().role() == requiredRole)) {
                    userAccountDao.recordSuccessfulLogin(username);
                    return Optional.of(new UserAccount(stored.get().username(), stored.get().role()));
                }
                if (stored.isPresent() && !locked) userAccountDao.recordFailedAttempt(username);
            } finally {
                Arrays.fill(password, '\0');
            }
            console.printf("Invalid credentials.%n");
        }
        console.printf("Too many failed attempts.%n");
        return Optional.empty();
    }

    private String readUsername(Console console) {
        while (true) {
            String username = console.readLine("New username (3-50 letters, numbers, dot, dash or underscore): ").trim();
            if (USERNAME_PATTERN.matcher(username).matches()) {
                return username;
            }
            console.printf("Invalid username format.%n");
        }
    }

    private ApplicationRole readRole(Console console) {
        ApplicationRole[] roles = ApplicationRole.values();
        console.printf("Select role:%n");
        for (int index = 0; index < roles.length; index++) {
            console.printf("%d. %s%n", index + 1, roles[index]);
        }
        while (true) {
            String raw = console.readLine("Role number: ").trim();
            try {
                int choice = Integer.parseInt(raw);
                if (choice >= 1 && choice <= roles.length) {
                    return roles[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                // Retry with a generic validation message.
            }
            console.printf("Choose one of the listed roles.%n");
        }
    }

    private char[] readConfirmedPassword(Console console) {
        while (true) {
            char[] password = console.readPassword("New password (at least 12 characters): ");
            char[] confirmation = console.readPassword("Confirm password: ");
            if (password == null || confirmation == null) {
                if (password != null) Arrays.fill(password, '\0');
                if (confirmation != null) Arrays.fill(confirmation, '\0');
                throw new IllegalStateException("A secure terminal is required to enter passwords.");
            }
            if (password.length >= 12 && Arrays.equals(password, confirmation)) {
                Arrays.fill(confirmation, '\0');
                return password;
            }
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            console.printf("Passwords must match and contain at least 12 characters.%n");
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
