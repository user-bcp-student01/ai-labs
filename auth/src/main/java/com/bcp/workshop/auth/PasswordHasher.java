package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordHasher {
    static final String HASH_PREFIX = "pbkdf2-sha256";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final String FIELD_SEPARATOR = "\\$";
    private static final String HASH_FIELD_SEPARATOR = "$";

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedKey = deriveKey(password, salt);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return String.join(HASH_FIELD_SEPARATOR,
                HASH_PREFIX,
                Integer.toString(ITERATIONS),
                encoder.encodeToString(salt),
                encoder.encodeToString(derivedKey));
    }

    public boolean matches(String password, String encodedHash) {
        if (password == null) {
            return false;
        }
        String[] fields = encodedHash == null ? new String[0] : encodedHash.split(FIELD_SEPARATOR, -1);
        if (fields.length != 4 || !HASH_PREFIX.equals(fields[0]) || !Integer.toString(ITERATIONS).equals(fields[1])) {
            return false;
        }

        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(fields[2]);
            byte[] expectedKey = decoder.decode(fields[3]);
            byte[] actualKey = deriveKey(password, salt);
            return MessageDigest.isEqual(expectedKey, actualKey);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public boolean isEncodedHash(String value) {
        return value != null && value.startsWith(HASH_PREFIX + HASH_FIELD_SEPARATOR);
    }

    private byte[] deriveKey(String password, byte[] salt) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password hashing is unavailable", error);
        } finally {
            keySpec.clearPassword();
        }
    }
}
