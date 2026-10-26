package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserRepository {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    private static final String CREATE_USERS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)";
    private static final String SELECT_ALL_PASSWORDS_SQL = "SELECT id, password FROM legacy_users";
    private static final String UPDATE_PASSWORD_SQL = "UPDATE legacy_users SET password=? WHERE id=?";
    private static final String SELECT_USER_SQL =
            "SELECT id, username, display_name, role, password FROM legacy_users WHERE username=?";
    private static final String INSERT_USER_SQL =
            "INSERT OR IGNORE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)";
    private static final String STUDENT_USERNAME = "student";
    private static final String STUDENT_PASSWORD = "Workshop123!";
    private static final String STUDENT_DISPLAY_NAME = "Workshop Student";
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String TRAINER_USERNAME = "trainer";
    private static final String TRAINER_PASSWORD = "Trainer123!";
    private static final String TRAINER_DISPLAY_NAME = "Workshop Trainer";
    private static final String TRAINER_ROLE = "TRAINER";

    @Inject
    PasswordHasher passwordHasher;

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_USERS_TABLE_SQL);
            }
            migratePlainTextPasswords();
            seedUser(STUDENT_USERNAME, STUDENT_PASSWORD, STUDENT_DISPLAY_NAME, STUDENT_ROLE);
            seedUser(TRAINER_USERNAME, TRAINER_PASSWORD, TRAINER_DISPLAY_NAME, TRAINER_ROLE);
        } catch (IOException | SQLException | RuntimeException error) {
            throw new AuthPersistenceException("Unable to initialize authentication storage", error);
        }
    }

    public UserRecord findByCredentials(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_SQL)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !passwordHasher.matches(password, result.getString("password"))) {
                    return null;
                }
                return new UserRecord(
                        result.getLong("id"),
                        result.getString("username"),
                        result.getString("display_name"),
                        result.getString("role")
                );
            }
        } catch (SQLException error) {
            throw new AuthPersistenceException("Unable to query authentication storage", error);
        }
    }

    private void migratePlainTextPasswords() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            List<StoredPassword> legacyPasswords = findPlainTextPasswords(connection);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD_SQL)) {
                for (StoredPassword legacyPassword : legacyPasswords) {
                    statement.setString(1, passwordHasher.hash(legacyPassword.password()));
                    statement.setLong(2, legacyPassword.userId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }
    }

    private List<StoredPassword> findPlainTextPasswords(Connection connection) throws SQLException {
        List<StoredPassword> legacyPasswords = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(SELECT_ALL_PASSWORDS_SQL)) {
            while (result.next()) {
                String storedPassword = result.getString("password");
                if (!passwordHasher.isEncodedHash(storedPassword)) {
                    legacyPasswords.add(new StoredPassword(result.getLong("id"), storedPassword));
                }
            }
        }
        return legacyPasswords;
    }

    private void seedUser(String username, String password, String displayName, String role) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement lookup = connection.prepareStatement("SELECT 1 FROM legacy_users WHERE username=?")) {
            lookup.setString(1, username);
            try (ResultSet result = lookup.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
            insertUser(connection, username, passwordHasher.hash(password), displayName, role);
        }
    }

    private void insertUser(Connection connection, String username, String password, String displayName, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, displayName);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private record StoredPassword(long userId, String password) {}
}
