package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.list_notes.ListNotesInputData;
import use_case.notes.list_notes.ListNotesInteractor;
import use_case.notes.list_notes.ListNotesOutputBoundary;
import use_case.notes.list_notes.ListNotesOutputData;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListNotesInteractorTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private RecordingPresenter presenter;
    private ListNotesInteractor interactor;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        presenter = new RecordingPresenter();
        interactor = new ListNotesInteractor(dataAccess, presenter);
        dataAccess.save(new Note("1", "Alpha", "body", List.of("CSC207"),
                Instant.parse("2026-11-27T10:00:00Z"), Instant.parse("2026-11-27T10:00:00Z")));
        dataAccess.save(new Note("2", "Beta", "alpha in body", List.of("Personal"),
                Instant.parse("2026-11-27T09:00:00Z"), Instant.parse("2026-11-27T09:00:00Z")));
    }

    @Test
    void returnsAllNotesAndTags() {
        interactor.execute(new ListNotesInputData());

        assertNull(presenter.error);
        assertEquals(2, presenter.output.getNotes().size());
        assertEquals(List.of("CSC207", "Personal"), presenter.output.getTags());
    }

    @Test
    void filtersByQueryAndTag() {
        interactor.execute(new ListNotesInputData("alpha", "Personal"));

        assertEquals(1, presenter.output.getNotes().size());
        assertEquals("2", presenter.output.getNotes().get(0).getId());
    }

    @Test
    void reportsReadFailure() {
        ListNotesInteractor failing = new ListNotesInteractor(new FailingNoteDataAccess(), presenter);

        failing.execute(new ListNotesInputData());

        assertEquals("Could not load notes. Try again.", presenter.error);
        assertNull(presenter.output);
    }

    private static final class RecordingPresenter implements ListNotesOutputBoundary {
        private ListNotesOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(ListNotesOutputData output) {
            this.output = output;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    private static final class FailingNoteDataAccess implements NoteDataAccessInterface {
        @Override
        public void save(Note note) {
            throw new AssertionError("save should not be called");
        }

        @Override
        public boolean update(Note note) {
            throw new AssertionError("update should not be called");
        }

        @Override
        public boolean delete(String noteId) {
            throw new AssertionError("delete should not be called");
        }

        @Override
        public java.util.Optional<Note> findById(String noteId) {
            throw new AssertionError("findById should not be called");
        }

        @Override
        public List<Note> search(String query, String tag) {
            throw new IllegalStateException("storage down");
        }

        @Override
        public List<String> findAllTags() {
            throw new AssertionError("findAllTags should not be called");
        }
    }
}
