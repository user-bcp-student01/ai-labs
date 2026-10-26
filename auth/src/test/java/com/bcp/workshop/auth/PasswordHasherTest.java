package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void storesSaltedHashesAndMatchesOnlyTheOriginalPassword() {
        String firstHash = passwordHasher.hash("Workshop123!");
        String secondHash = passwordHasher.hash("Workshop123!");

        assertTrue(passwordHasher.isEncodedHash(firstHash));
        assertTrue(passwordHasher.matches("Workshop123!", firstHash));
        assertFalse(passwordHasher.matches("incorrect", firstHash));
        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void rejectsMalformedOrUnsupportedHashes() {
        assertFalse(passwordHasher.matches(null, passwordHasher.hash("password")));
        assertFalse(passwordHasher.matches("password", null));
        assertFalse(passwordHasher.matches("password", "not-a-hash"));
        assertFalse(passwordHasher.matches("password", "pbkdf2-sha256$1$salt$hash"));
        assertFalse(passwordHasher.matches("password", "pbkdf2-sha256$600000$%%%$hash"));
    }
}
