package data_access;

import entity.Memory;
import use_case.agent.AgentDataAccessInterface;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySqlDataAccess implements AgentDataAccessInterface {
    private final String url;
    private final String user;
    private final String password;

    public MySqlDataAccess(String url, String user, String password) {
        this.url = url == null ? "" : url.trim();
        this.user = user == null ? "" : user.trim();
        this.password = password == null ? "" : password;
    }

    public static MySqlDataAccess fromEnvironment() {
        Map<String, String> file = readDotEnv(Path.of(".env"));
        return new MySqlDataAccess(
                setting(file, "MYSQL_URL"),
                setting(file, "MYSQL_USER"),
                setting(file, "MYSQL_PASSWORD"));
    }

    @Override
    public void save(Memory memory) {
        String sql = "INSERT INTO memory (id, content, source) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE content = VALUES(content), source = VALUES(source)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memory.getId());
            statement.setString(2, memory.getContent());
            statement.setString(3, memory.getSource());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the memory to MySQL.");
        }
    }

    @Override
    public List<Memory> findRelevant(String query) {
        String sql = "SELECT id, content, source FROM memory WHERE id = ? OR content LIKE ?";
        List<Memory> memories = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, query);
            statement.setString(2, "%" + query + "%");
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    memories.add(new Memory(results.getString("id"), results.getString("content"), results.getString("source")));
                }
            }
            return memories;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not search memories in MySQL.");
        }
    }

    public Memory findById(String id) {
        String sql = "SELECT id, content, source FROM memory WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new Memory(results.getString("id"), results.getString("content"), results.getString("source"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the memory from MySQL.");
        }
    }

    private Connection connect() throws SQLException {
        if (url.isBlank()) {
            throw new IllegalStateException("MYSQL_URL is not set.");
        }
        Connection connection = DriverManager.getConnection(url, user, password);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS memory ("
                            + "id VARCHAR(64) PRIMARY KEY,"
                            + "content TEXT NOT NULL,"
                            + "source VARCHAR(32))");
        }
        return connection;
    }

    private static String setting(Map<String, String> file, String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = file.get(key);
        return fromFile == null ? "" : fromFile.trim();
    }

    private static Map<String, String> readDotEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        } catch (IOException exception) {
            return values;
        }
        return values;
    }
}
