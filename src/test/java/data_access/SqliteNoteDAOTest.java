package data_access;

import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteNoteDAOTest {
    @TempDir
    Path tempDir;

    private Path databaseFile;
    private SqliteNoteDAO dao;

    @BeforeEach
    void setUp() {
        databaseFile = tempDir.resolve("notes.db");
        SqliteDatabase database = new SqliteDatabase(databaseFile);
        database.initializeSchema();
        dao = new SqliteNoteDAO(database);
    }

    @Test
    void savesAndReadsANoteWithTags() {
        Note note = note("1", "Exam prep", "# Derivatives\n\n- chain rule",
                List.of("CSC207", "Math"), Instant.parse("2026-11-27T10:00:00Z"));

        dao.save(note);

        Optional<Note> loaded = dao.findById("1");
        assertTrue(loaded.isPresent());
        assertEquals("Exam prep", loaded.get().getTitle());
        assertEquals("# Derivatives\n\n- chain rule", loaded.get().getMarkdown());
        assertEquals(List.of("CSC207", "Math"), loaded.get().getTags());
        assertEquals(note.getCreatedAt(), loaded.get().getCreatedAt());
        assertEquals(note.getUpdatedAt(), loaded.get().getUpdatedAt());
    }

    @Test
    void updateReplacesFieldsAndTags() {
        Note original = note("1", "Old", "old body", List.of("old"),
                Instant.parse("2026-11-27T10:00:00Z"));
        dao.save(original);
        Note updated = new Note("1", "New", "new body", List.of("new", "CSC207"),
                original.getCreatedAt(), Instant.parse("2026-11-27T11:00:00Z"));

        assertTrue(dao.update(updated));

        Note loaded = dao.findById("1").orElseThrow();
        assertEquals("New", loaded.getTitle());
        assertEquals("new body", loaded.getMarkdown());
        assertEquals(List.of("CSC207", "new"), loaded.getTags());
        assertEquals(original.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(updated.getUpdatedAt(), loaded.getUpdatedAt());
    }

    @Test
    void updateReturnsFalseForAnUnknownId() {
        Note note = note("missing", "Missing", "", List.of(),
                Instant.parse("2026-11-27T10:00:00Z"));

        assertFalse(dao.update(note));
    }

    @Test
    void deleteRemovesTheNoteAndItsTags() {
        dao.save(note("1", "Temporary", "", List.of("delete-me"),
                Instant.parse("2026-11-27T10:00:00Z")));

        assertTrue(dao.delete("1"));

        assertTrue(dao.findById("1").isEmpty());
        assertTrue(dao.findAllTags().isEmpty());
        assertFalse(dao.delete("1"));
    }

    @Test
    void rejectsADuplicateNoteId() {
        Note note = note("1", "First", "", List.of(),
                Instant.parse("2026-11-27T10:00:00Z"));
        dao.save(note);

        assertThrows(IllegalStateException.class, () -> dao.save(note));
        assertEquals("First", dao.findById("1").orElseThrow().getTitle());
    }

    @Test
    void searchMatchesTitleAndMarkdownCaseInsensitively() {
        dao.save(note("1", "Alpha title", "body", List.of(),
                Instant.parse("2026-11-27T10:00:00Z")));
        dao.save(note("2", "Other", "mentions ALPHA in the body", List.of(),
                Instant.parse("2026-11-27T09:00:00Z")));
        dao.save(note("3", "Unrelated", "nothing", List.of(),
                Instant.parse("2026-11-27T08:00:00Z")));

        List<Note> results = dao.search("alpha", "");

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("2", results.get(1).getId());
    }

    @Test
    void searchTreatsPercentAndUnderscoreLiterally() {
        dao.save(note("1", "Progress", "100%_done", List.of(),
                Instant.parse("2026-11-27T10:00:00Z")));
        dao.save(note("2", "Other", "100x done", List.of(),
                Instant.parse("2026-11-27T09:00:00Z")));

        List<Note> results = dao.search("%_", "");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getId());
    }

    @Test
    void filtersByTagCaseInsensitively() {
        dao.save(note("1", "One", "", List.of("CSC207", "Study"),
                Instant.parse("2026-11-27T10:00:00Z")));
        dao.save(note("2", "Two", "", List.of("Personal"),
                Instant.parse("2026-11-27T09:00:00Z")));

        List<Note> results = dao.search("", "csc207");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getId());
    }

    @Test
    void searchReturnsAllNotesInUpdatedOrderWhenFiltersAreBlank() {
        dao.save(note("1", "Oldest", "", List.of(),
                Instant.parse("2026-11-27T08:00:00Z")));
        dao.save(note("2", "Newest", "", List.of(),
                Instant.parse("2026-11-27T10:00:00Z")));
        dao.save(note("3", "Middle", "", List.of(),
                Instant.parse("2026-11-27T09:00:00Z")));

        List<Note> results = dao.search("", "");

        assertEquals(List.of("2", "3", "1"),
                results.stream().map(Note::getId).collect(Collectors.toList()));
    }

    @Test
    void persistsAcrossDaoInstances() {
        dao.save(note("1", "Persistent", "body", List.of("tag"),
                Instant.parse("2026-11-27T10:00:00Z")));

        SqliteDatabase reopened = new SqliteDatabase(databaseFile);
        SqliteNoteDAO second = new SqliteNoteDAO(reopened);

        assertTrue(second.findById("1").isPresent());
        assertEquals("Persistent", second.findById("1").orElseThrow().getTitle());
    }

    @Test
    void findAllTagsReturnsDistinctTagsAlphabetically() {
        dao.save(note("1", "One", "", List.of("b", "A"),
                Instant.parse("2026-11-27T10:00:00Z")));
        dao.save(note("2", "Two", "", List.of("a", "c"),
                Instant.parse("2026-11-27T09:00:00Z")));

        List<String> tags = dao.findAllTags();

        assertEquals(3, tags.size());
        assertEquals(List.of("a", "b", "c"),
                tags.stream().map(String::toLowerCase).collect(Collectors.toList()));
    }

    @Test
    void findByIdReturnsEmptyForBlankOrUnknownIds() {
        assertTrue(dao.findById("").isEmpty());
        assertTrue(dao.findById("   ").isEmpty());
        assertTrue(dao.findById("unknown").isEmpty());
    }

    private static Note note(String id, String title, String markdown, List<String> tags,
                             Instant updatedAt) {
        return new Note(id, title, markdown, tags, updatedAt, updatedAt);
    }
}
