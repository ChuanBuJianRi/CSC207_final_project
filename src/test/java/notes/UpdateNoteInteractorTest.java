package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.update_note.UpdateNoteInputData;
import use_case.notes.update_note.UpdateNoteInteractor;
import use_case.notes.update_note.UpdateNoteOutputBoundary;
import use_case.notes.update_note.UpdateNoteOutputData;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateNoteInteractorTest {
    private static final Instant CREATED = Instant.parse("2020-01-01T00:00:00Z");

    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private RecordingPresenter presenter;
    private UpdateNoteInteractor interactor;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        dataAccess.save(new Note("1", "Old title", "old body", List.of("old"),
                CREATED, CREATED));
        presenter = new RecordingPresenter();
        interactor = new UpdateNoteInteractor(dataAccess, presenter);
    }

    @Test
    void updatesAllFieldsAndKeepsTheCreationTime() {
        interactor.execute(new UpdateNoteInputData(
                "1", "New title", "new body", List.of("CSC207", " csc207 ")));

        assertNull(presenter.error);
        Note note = presenter.output.getNote();
        assertEquals("New title", note.getTitle());
        assertEquals("new body", note.getMarkdown());
        assertEquals(List.of("CSC207"), note.getTags());
        assertEquals(CREATED, note.getCreatedAt());
        assertTrue(note.getUpdatedAt().isAfter(CREATED));
        assertEquals("New title", dataAccess.findById("1").orElseThrow().getTitle());
    }

    @Test
    void keepsOmittedFields() {
        interactor.execute(new UpdateNoteInputData("1", null, "new body", null));

        Note note = presenter.output.getNote();
        assertEquals("Old title", note.getTitle());
        assertEquals("new body", note.getMarkdown());
        assertEquals(List.of("old"), note.getTags());
    }

    @Test
    void canReplaceTagsOnly() {
        interactor.execute(new UpdateNoteInputData("1", null, null, List.of("new")));

        assertEquals("Old title", presenter.output.getNote().getTitle());
        assertEquals(List.of("new"), presenter.output.getNote().getTags());
    }

    @Test
    void rejectsABlankTitle() {
        interactor.execute(new UpdateNoteInputData("1", "  ", null, null));

        assertEquals("Enter a note title.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void rejectsAnUnknownNote() {
        interactor.execute(new UpdateNoteInputData("missing", "Title", null, null));

        assertEquals("Note not found.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void rejectsAnEmptyUpdate() {
        interactor.execute(new UpdateNoteInputData("1", null, null, null));

        assertEquals("Provide a note title, markdown, or tags to update.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void reportsStorageFailureWhileLoading() {
        UpdateNoteInteractor failing = new UpdateNoteInteractor(
                new FailingNoteDataAccess(true), presenter);

        failing.execute(new UpdateNoteInputData("1", "Title", null, null));

        assertEquals("Could not load the note. Try again.", presenter.error);
    }

    @Test
    void reportsStorageFailureWhileUpdating() {
        UpdateNoteInteractor failing = new UpdateNoteInteractor(
                new FailingNoteDataAccess(false), presenter);

        failing.execute(new UpdateNoteInputData("1", "Title", null, null));

        assertEquals("Could not update the note. Try again.", presenter.error);
    }

    private static final class RecordingPresenter implements UpdateNoteOutputBoundary {
        private UpdateNoteOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(UpdateNoteOutputData output) {
            this.output = output;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    private static final class FailingNoteDataAccess implements NoteDataAccessInterface {
        private final boolean failOnLoad;

        private FailingNoteDataAccess(boolean failOnLoad) {
            this.failOnLoad = failOnLoad;
        }

        @Override
        public void save(Note note) {
            throw new AssertionError("save should not be called");
        }

        @Override
        public boolean update(Note note) {
            if (failOnLoad) {
                throw new AssertionError("update should not be called");
            }
            throw new IllegalStateException("storage down");
        }

        @Override
        public boolean delete(String noteId) {
            throw new AssertionError("delete should not be called");
        }

        @Override
        public Optional<Note> findById(String noteId) {
            if (failOnLoad) {
                throw new IllegalStateException("storage down");
            }
            return Optional.of(new Note("1", "Title", "", List.of(), CREATED, CREATED));
        }

        @Override
        public List<Note> search(String query, String tag) {
            throw new AssertionError("search should not be called");
        }

        @Override
        public List<String> findAllTags() {
            throw new AssertionError("findAllTags should not be called");
        }
    }
}
