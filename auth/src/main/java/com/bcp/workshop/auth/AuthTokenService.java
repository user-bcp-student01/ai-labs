package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@ApplicationScoped
public class AuthTokenService {
    static final int EXPIRATION_SECONDS = 3600;
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String CLAIM_SUBJECT = "sub";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ISSUED_AT = "iat";
    private static final String CLAIM_EXPIRATION = "exp";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder TOKEN_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ENCODED_HEADER =
            TOKEN_ENCODER.encodeToString(TOKEN_HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final byte[] signingSecret;

    @Inject
    public AuthTokenService(@ConfigProperty(name = "auth.token.secret") String signingSecret) {
        if (signingSecret == null || signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Token signing secret must contain at least 32 bytes");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String create(String username, String role) {
        return createAt(username, role, Instant.now());
    }

    String createAt(String username, String role, Instant issuedAt) {
        long issuedAtEpochSeconds = issuedAt.getEpochSecond();
        long expirationEpochSeconds = issuedAtEpochSeconds + EXPIRATION_SECONDS;
        Map<String, Object> claims = Map.of(
                CLAIM_SUBJECT, username,
                CLAIM_ROLE, role,
                CLAIM_ISSUED_AT, issuedAtEpochSeconds,
                CLAIM_EXPIRATION, expirationEpochSeconds
        );
        try {
            String encodedClaims = TOKEN_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(claims));
            String signingInput = ENCODED_HEADER + "." + encodedClaims;
            String encodedSignature = TOKEN_ENCODER.encodeToString(sign(signingInput));
            return signingInput + "." + encodedSignature;
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize token claims", error);
        }
    }

    public TokenClaims verify(String token) {
        if (token == null) {
            throw invalidToken();
        }
        String[] tokenParts = token.split("\\.", -1);
        if (tokenParts.length != 3 || !ENCODED_HEADER.equals(tokenParts[0])) {
            throw invalidToken();
        }

        byte[] submittedSignature = decode(tokenParts[2]);
        byte[] expectedSignature = sign(tokenParts[0] + "." + tokenParts[1]);
        if (!MessageDigest.isEqual(expectedSignature, submittedSignature)) {
            throw invalidToken();
        }

        JsonNode claims = readClaims(tokenParts[1]);
        if (claims == null || !claims.isObject()) {
            throw invalidToken();
        }
        JsonNode subject = claims.get(CLAIM_SUBJECT);
        JsonNode role = claims.get(CLAIM_ROLE);
        JsonNode issuedAt = claims.get(CLAIM_ISSUED_AT);
        JsonNode expiration = claims.get(CLAIM_EXPIRATION);
        if (subject == null || !subject.isTextual() || role == null || !role.isTextual()
                || issuedAt == null || !issuedAt.canConvertToLong()
                || expiration == null || !expiration.canConvertToLong()) {
            throw invalidToken();
        }

        long issuedAtEpochSeconds = issuedAt.longValue();
        long expirationEpochSeconds = expiration.longValue();
        long currentEpochSeconds = Instant.now().getEpochSecond();
        if (issuedAtEpochSeconds > currentEpochSeconds || expirationEpochSeconds <= currentEpochSeconds) {
            throw invalidToken();
        }
        return new TokenClaims(subject.textValue(), role.textValue(), issuedAtEpochSeconds, expirationEpochSeconds);
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, SIGNATURE_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Token signing is unavailable", error);
        }
    }

    private byte[] decode(String encodedValue) {
        try {
            return TOKEN_DECODER.decode(encodedValue);
        } catch (IllegalArgumentException error) {
            throw invalidToken();
        }
    }

    private JsonNode readClaims(String encodedClaims) {
        try {
            return OBJECT_MAPPER.readTree(decode(encodedClaims));
        } catch (IOException error) {
            throw invalidToken();
        }
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
    }

    public record TokenClaims(String subject, String role, long issuedAt, long expiresAt) {}
}
