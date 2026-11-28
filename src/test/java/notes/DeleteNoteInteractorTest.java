package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.delete_note.DeleteNoteInputData;
import use_case.notes.delete_note.DeleteNoteInteractor;
import use_case.notes.delete_note.DeleteNoteOutputBoundary;
import use_case.notes.delete_note.DeleteNoteOutputData;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteNoteInteractorTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private RecordingPresenter presenter;
    private DeleteNoteInteractor interactor;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        dataAccess.save(new Note("1", "Title", "", List.of("tag"),
                Instant.parse("2026-11-27T10:00:00Z"), Instant.parse("2026-11-27T10:00:00Z")));
        presenter = new RecordingPresenter();
        interactor = new DeleteNoteInteractor(dataAccess, presenter);
    }

    @Test
    void deletesAnExistingNote() {
        interactor.execute(new DeleteNoteInputData("1"));

        assertNull(presenter.error);
        assertEquals("1", presenter.output.getNoteId());
        assertTrue(dataAccess.findById("1").isEmpty());
    }

    @Test
    void rejectsABlankId() {
        interactor.execute(new DeleteNoteInputData("  "));

        assertEquals("Select a note.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void reportsAnUnknownId() {
        interactor.execute(new DeleteNoteInputData("missing"));

        assertEquals("Note not found.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void reportsStorageFailure() {
        DeleteNoteInteractor failing = new DeleteNoteInteractor(new FailingNoteDataAccess(), presenter);

        failing.execute(new DeleteNoteInputData("1"));

        assertEquals("Could not delete the note. Try again.", presenter.error);
        assertNull(presenter.output);
    }

    private static final class RecordingPresenter implements DeleteNoteOutputBoundary {
        private DeleteNoteOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(DeleteNoteOutputData output) {
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
            throw new IllegalStateException("storage down");
        }

        @Override
        public Optional<Note> findById(String noteId) {
            throw new AssertionError("findById should not be called");
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
