package data_access;

import entity.Note;
import use_case.notes.NoteDataAccessInterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores notes and their tags in the local SQLite database.
 */
public final class SqliteNoteDAO implements NoteDataAccessInterface {
    private static final DateTimeFormatter TIMESTAMP =
            new DateTimeFormatterBuilder().appendInstant(9).toFormatter();

    private final SqliteDatabase database;

    public SqliteNoteDAO(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public void save(Note note) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                insertNote(connection, note);
                replaceTags(connection, note);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new IllegalStateException("Could not save the note.", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the note.", exception);
        }
    }

    @Override
    public boolean update(Note note) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE note SET title = ?, markdown = ?, updated_at = ? WHERE id = ?")) {
                    statement.setString(1, note.getTitle());
                    statement.setString(2, note.getMarkdown());
                    statement.setString(3, TIMESTAMP.format(note.getUpdatedAt()));
                    statement.setString(4, note.getId());
                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return false;
                    }
                }
                replaceTags(connection, note);
                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new IllegalStateException("Could not update the note.", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update the note.", exception);
        }
    }

    @Override
    public boolean delete(String noteId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM note WHERE id = ?")) {
            statement.setString(1, noteId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete the note.", exception);
        }
    }

    @Override
    public Optional<Note> findById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, title, markdown, created_at, updated_at FROM note WHERE id = ?")) {
            statement.setString(1, noteId);
            NoteRow row;
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return Optional.empty();
                }
                row = new NoteRow(results);
            }
            return Optional.of(row.toNote(connection));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the note.", exception);
        }
    }

    @Override
    public List<Note> search(String query, String tag) {
        String text = query == null ? "" : query.trim();
        String selectedTag = tag == null ? "" : tag.trim();
        StringBuilder sql = new StringBuilder(
                "SELECT n.id, n.title, n.markdown, n.created_at, n.updated_at FROM note n");
        List<String> conditions = new ArrayList<>();
        if (!text.isEmpty()) {
            conditions.add("(instr(lower(n.title), lower(?)) > 0"
                    + " OR instr(lower(n.markdown), lower(?)) > 0)");
        }
        if (!selectedTag.isEmpty()) {
            conditions.add("EXISTS (SELECT 1 FROM note_tag t"
                    + " WHERE t.note_id = n.id AND t.tag = ? COLLATE NOCASE)");
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY n.updated_at DESC, n.id ASC");

        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (!text.isEmpty()) {
                statement.setString(parameter++, text);
                statement.setString(parameter++, text);
            }
            if (!selectedTag.isEmpty()) {
                statement.setString(parameter, selectedTag);
            }
            List<NoteRow> rows = new ArrayList<>();
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    rows.add(new NoteRow(results));
                }
            }
            List<Note> notes = new ArrayList<>();
            for (NoteRow row : rows) {
                notes.add(row.toNote(connection));
            }
            return notes;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not search notes.", exception);
        }
    }

    @Override
    public List<String> findAllTags() {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT tag FROM note_tag GROUP BY tag COLLATE NOCASE"
                             + " ORDER BY tag COLLATE NOCASE");
             ResultSet results = statement.executeQuery()) {
            List<String> tags = new ArrayList<>();
            while (results.next()) {
                tags.add(results.getString("tag"));
            }
            return tags;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read note tags.", exception);
        }
    }

    private static void insertNote(Connection connection, Note note) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO note (id, title, markdown, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, note.getId());
            statement.setString(2, note.getTitle());
            statement.setString(3, note.getMarkdown());
            statement.setString(4, TIMESTAMP.format(note.getCreatedAt()));
            statement.setString(5, TIMESTAMP.format(note.getUpdatedAt()));
            statement.executeUpdate();
        }
    }

    private static void replaceTags(Connection connection, Note note) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM note_tag WHERE note_id = ?")) {
            delete.setString(1, note.getId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO note_tag (note_id, tag) VALUES (?, ?)")) {
            for (String tag : note.getTags()) {
                insert.setString(1, note.getId());
                insert.setString(2, tag);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static final class NoteRow {
        private final String id;
        private final String title;
        private final String markdown;
        private final Instant createdAt;
        private final Instant updatedAt;

        private NoteRow(ResultSet results) throws SQLException {
            this.id = results.getString("id");
            this.title = results.getString("title");
            this.markdown = results.getString("markdown");
            this.createdAt = parseTimestamp(results.getString("created_at"));
            this.updatedAt = parseTimestamp(results.getString("updated_at"));
        }

        private static Instant parseTimestamp(String value) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException exception) {
                throw new IllegalStateException(
                        "The local database contains an invalid note timestamp.", exception);
            }
        }

        private Note toNote(Connection connection) throws SQLException {
            return new Note(id, title, markdown, tagsFor(connection, id), createdAt, updatedAt);
        }
    }

    private static List<String> tagsFor(Connection connection, String noteId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT tag FROM note_tag WHERE note_id = ? ORDER BY tag COLLATE NOCASE")) {
            statement.setString(1, noteId);
            try (ResultSet results = statement.executeQuery()) {
                List<String> tags = new ArrayList<>();
                while (results.next()) {
                    tags.add(results.getString("tag"));
                }
                return tags;
            }
        }
    }

    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
