package data_access;

import entity.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteMemoryDAOTest {
    @TempDir
    Path tempDir;

    private Path databaseFile;
    private SqliteMemoryDAO dao;

    @BeforeEach
    void setUp() {
        databaseFile = tempDir.resolve("notes.db");
        SqliteDatabase database = new SqliteDatabase(databaseFile);
        database.initializeSchema();
        dao = new SqliteMemoryDAO(database);
    }

    @Test
    void savesAndFindsMatchingMemories() {
        dao.save(new Memory("1", "The exam is at 9am", "user"));
        dao.save(new Memory("2", "Unrelated", "assistant"));

        List<Memory> results = dao.findRelevant("exam");

        assertEquals(1, results.size());
        assertEquals("The exam is at 9am", results.get(0).getContent());
        assertEquals("user", results.get(0).getSource());
    }

    @Test
    void returnsNewestMatchesFirst() {
        dao.save(new Memory("1", "first note", "user"));
        dao.save(new Memory("2", "second note", "user"));
        dao.save(new Memory("3", "third note", "user"));

        List<Memory> results = dao.findRelevant("note");

        assertEquals(List.of("3", "2", "1"),
                results.stream().map(Memory::getId).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void returnsEmptyWhenNothingMatches() {
        dao.save(new Memory("1", "exam", "user"));

        assertTrue(dao.findRelevant("holiday").isEmpty());
    }

    @Test
    void replacesAMemoryWithTheSameId() {
        dao.save(new Memory("1", "old", "user"));
        dao.save(new Memory("1", "new", "assistant"));

        List<Memory> results = dao.findRelevant("new");

        assertEquals(1, results.size());
        assertEquals("new", results.get(0).getContent());
        assertEquals("assistant", results.get(0).getSource());
    }

    @Test
    void limitsResultsToTwenty() {
        for (int index = 0; index < 25; index++) {
            dao.save(new Memory(String.valueOf(index), "memory " + index, "user"));
        }

        assertEquals(20, dao.findRelevant("memory").size());
    }

    @Test
    void persistsAcrossDaoInstances() {
        dao.save(new Memory("1", "persistent memory", "user"));

        SqliteDatabase reopened = new SqliteDatabase(databaseFile);
        SqliteMemoryDAO second = new SqliteMemoryDAO(reopened);

        assertEquals(1, second.findRelevant("persistent").size());
    }
}
