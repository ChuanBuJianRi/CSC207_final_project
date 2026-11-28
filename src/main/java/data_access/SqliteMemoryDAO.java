package data_access;

import entity.Memory;
import use_case.agent.AgentDataAccessInterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores agent memories in the local SQLite database.
 */
public final class SqliteMemoryDAO implements AgentDataAccessInterface {
    private static final int RESULT_LIMIT = 20;

    private final SqliteDatabase database;

    public SqliteMemoryDAO(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public void save(Memory memory) {
        String sql = "INSERT INTO memory (id, content, source) VALUES (?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET content = excluded.content, source = excluded.source";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memory.getId());
            statement.setString(2, memory.getContent());
            statement.setString(3, memory.getSource());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the memory.", exception);
        }
    }

    @Override
    public List<Memory> findRelevant(String query) {
        String text = query == null ? "" : query.trim();
        String sql;
        if (text.isEmpty()) {
            sql = "SELECT id, content, source FROM memory ORDER BY rowid DESC LIMIT ?";
        } else {
            sql = "SELECT id, content, source FROM memory"
                    + " WHERE instr(lower(content), lower(?)) > 0"
                    + " ORDER BY rowid DESC LIMIT ?";
        }
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            if (!text.isEmpty()) {
                statement.setString(parameter++, text);
            }
            statement.setInt(parameter, RESULT_LIMIT);
            List<Memory> memories = new ArrayList<>();
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    memories.add(new Memory(
                            results.getString("id"),
                            results.getString("content"),
                            results.getString("source")));
                }
            }
            return memories;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not search memories.", exception);
        }
    }
}
