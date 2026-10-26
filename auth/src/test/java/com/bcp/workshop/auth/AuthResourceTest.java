package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    private static final String DATABASE_URL =
            System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");

    @Inject
    AuthTokenService tokenService;

    @Test
    void logsInWithWorkshopUser() {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("token", notNullValue())
                .extract()
                .path("token");

        AuthTokenService.TokenClaims claims = tokenService.verify(token);
        assertEquals("student", claims.subject());
        assertEquals("STUDENT", claims.role());
        assertEquals(3600, claims.expiresAt() - claims.issuedAt());
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("$", equalTo(Map.of("message", "Invalid user student or password")));
    }

    @Test
    void treatsQuoteInUnknownUsernameAsCredentialData() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown'user\",\"password\":\"incorrect\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("$", equalTo(Map.of("message", "Invalid user unknown'user or password")));
    }

    @Test
    void rejectsMissingCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\" \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", equalTo(Map.of("message", "Password is required")));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\" \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", equalTo(Map.of("message", "Username is required")));
    }

    @Test
    void rejectsNullUsername() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", equalTo(Map.of("message", "Username is required")));
    }

    @Test
    void rejectsNullPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", equalTo(Map.of("message", "Password is required")));
    }

    @Test
    void rejectsNullRequestWithDocumentedErrorBody() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", equalTo(Map.of("message", "Request is null")));
    }

    @Test
    void returnsGenericErrorBodyWhenAuthenticationStorageIsUnavailable() throws Exception {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE legacy_users RENAME TO legacy_users_unavailable");
            try {
                given()
                        .contentType("application/json")
                        .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                        .when().post("/auth/login")
                        .then()
                        .statusCode(500)
                        .body("$", equalTo(Map.of(
                                "message", "Authentication failed",
                                "exception", "InternalServerError"
                        )));
            } finally {
                statement.executeUpdate("ALTER TABLE legacy_users_unavailable RENAME TO legacy_users");
            }
        }
    }

    @Test
    void reportsHealthUpWithoutAuthenticationAndWithoutRegisteredChecks() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.size()", is(0));
    }

    @Test
    void rejectsUnlistedCorsOrigin() {
        given()
                .header("Origin", "https://unlisted.example")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void allowsConfiguredDevelopmentCorsOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", is("http://localhost:4200"));
    }

    @Test
    void storesSeededPasswordAsSaltedHashAfterMigration() throws Exception {
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT password FROM legacy_users WHERE username='student'")) {
            assertTrue(result.next());
            assertTrue(new PasswordHasher().isEncodedHash(result.getString("password")));
            assertTrue(new PasswordHasher().matches("Workshop123!", result.getString("password")));
        }
    }

    @Test
    void migratesExistingPlainTextPasswordAndPreservesLogin() throws Exception {
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT OR REPLACE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)")) {
            statement.setString(1, "legacy-migration");
            statement.setString(2, "LegacyPass123!");
            statement.setString(3, "Legacy User");
            statement.setString(4, "STUDENT");
            statement.executeUpdate();
        }

        UserRepository repository = new UserRepository();
        repository.passwordHasher = new PasswordHasher();
        repository.initialize();

        assertNotNull(repository.findByCredentials("legacy-migration", "LegacyPass123!"));
        assertNull(repository.findByCredentials("legacy-migration", "wrong"));
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password FROM legacy_users WHERE username=?")) {
            statement.setString(1, "legacy-migration");
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                String storedPassword = result.getString("password");
                assertNotEquals("LegacyPass123!", storedPassword);
                assertTrue(repository.passwordHasher.isEncodedHash(storedPassword));
            }
        }
    }

    @Test
    void wrapsInitializationFailuresWithoutSuppressingThem() {
        UserRepository repository = new UserRepository();

        AuthPersistenceException error = assertThrows(
                AuthPersistenceException.class,
                repository::initialize
        );

        assertEquals("Unable to initialize authentication storage", error.getMessage());
        assertNotNull(error.getCause());
    }

    @Test
    void wrapsQueryFailuresInsteadOfTreatingThemAsInvalidCredentials() throws Exception {
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        UserRepository repository = new UserRepository();
        repository.passwordHasher = new PasswordHasher();

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE legacy_users RENAME TO legacy_users_unavailable");
            try {
                assertThrows(
                        AuthPersistenceException.class,
                        () -> repository.findByCredentials("student", "Workshop123!")
                );
            } finally {
                statement.executeUpdate("ALTER TABLE legacy_users_unavailable RENAME TO legacy_users");
            }
        }
    }

    @Test
    void returnsGenericMessageWhenAuthenticationFailsInternally() {
        AuthResource resource = new AuthResource();
        resource.authService = new AuthService() {
            @Override
            public UserRecord authenticate(String username, String password) {
                throw new IllegalStateException("sensitive test failure");
            }
        };
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        try (Response response = resource.login(request)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getEntity();

            assertEquals(500, response.getStatus());
            assertEquals("Authentication failed", body.get("message"));
            assertEquals("InternalServerError", body.get("exception"));
            assertFalse(body.toString().contains("sensitive test failure"));
        }
    }

    @Test
    void rejectsNullRequestWithoutCallingAuthenticationService() {
        AuthResource resource = new AuthResource();

        try (Response response = resource.login(null)) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void mapsUserToEscapedResponseDto() {
        AuthResource resource = new AuthResource();
        resource.tokenService = new AuthTokenService("test-only-auth-token-secret-for-isolated-unit-tests");
        resource.authService = new AuthService() {
            @Override
            public UserRecord authenticate(String username, String password) {
                return new UserRecord(1, username, "<student>", "STUDENT");
            }
        };
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        try (Response response = resource.login(request)) {
            LoginResponse body = (LoginResponse) response.getEntity();

            assertEquals(200, response.getStatus());
            assertEquals("&lt;student&gt;", body.user().displayName());
            assertEquals("STUDENT", body.user().role());
        }
    }
}
