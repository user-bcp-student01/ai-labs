package com.bcp.workshop.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class LegacyTokenUtil {
    private static final String SECRET = "WORKSHOP_SECRET_2026";

    private LegacyTokenUtil() {}

    public static String create(String username, String role) {
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        String value = username + "|" + role + "|" + expiry + "|" + SECRET;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
