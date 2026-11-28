package data_access;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the single local SQLite database used by the app and creates its schema.
 */
public final class SqliteDatabase {
    public static final int SCHEMA_VERSION = 1;
    public static final String DEFAULT_PATH = "data/notes.db";
    public static final String PATH_ENV = "NOTES_DB_PATH";

    private final Path path;

    public SqliteDatabase(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public static SqliteDatabase fromEnvironment() {
        String configured = System.getenv(PATH_ENV);
        String value = configured == null || configured.isBlank()
                ? DEFAULT_PATH : configured.trim();
        return new SqliteDatabase(Path.of(value));
    }

    public Path getPath() {
        return path;
    }

    /**
     * Opens a connection with the settings every DAO needs.
     */
    public Connection connect() {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the local database folder.", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not open the local database.", exception);
        }
    }

    /**
     * Creates the note, tag and memory tables if they do not exist yet.
     * Safe to call more than once.
     */
    public void initializeSchema() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("CREATE TABLE IF NOT EXISTS note ("
                    + "id TEXT PRIMARY KEY,"
                    + "title TEXT NOT NULL,"
                    + "markdown TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS note_tag ("
                    + "note_id TEXT NOT NULL REFERENCES note(id) ON DELETE CASCADE,"
                    + "tag TEXT COLLATE NOCASE NOT NULL,"
                    + "PRIMARY KEY (note_id, tag))");
            statement.execute("CREATE TABLE IF NOT EXISTS memory ("
                    + "id TEXT PRIMARY KEY,"
                    + "content TEXT NOT NULL,"
                    + "source TEXT)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_note_updated"
                    + " ON note(updated_at DESC)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_note_tag_tag"
                    + " ON note_tag(tag)");
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize the local database.", exception);
        }
    }
}
