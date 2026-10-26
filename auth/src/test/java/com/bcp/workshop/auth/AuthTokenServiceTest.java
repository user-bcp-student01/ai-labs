package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenServiceTest {
    private final AuthTokenService tokenService =
            new AuthTokenService("test-only-auth-token-secret-for-isolated-unit-tests");

    @Test
    void createsSignedTokenWithIdentityAndExpiryClaims() {
        String token = tokenService.create("student", "STUDENT");
        AuthTokenService.TokenClaims claims = tokenService.verify(token);

        assertEquals("student", claims.subject());
        assertEquals("STUDENT", claims.role());
        assertEquals(AuthTokenService.EXPIRATION_SECONDS, claims.expiresAt() - claims.issuedAt());
        assertTrue(claims.expiresAt() > Instant.now().getEpochSecond());
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        String token = tokenService.create("student", "STUDENT");
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalid-signature";
        String expiredToken = tokenService.createAt(
                "student",
                "STUDENT",
                Instant.now().minusSeconds(AuthTokenService.EXPIRATION_SECONDS * 2L)
        );

        assertThrows(IllegalArgumentException.class, () -> tokenService.verify(tamperedToken));
        assertThrows(IllegalArgumentException.class, () -> tokenService.verify(expiredToken));
    }

    @Test
    void rejectsMissingOrWeakSigningSecrets() {
        assertThrows(IllegalArgumentException.class, () -> new AuthTokenService(null));
        assertThrows(IllegalArgumentException.class, () -> new AuthTokenService("too-short"));
    }
}
