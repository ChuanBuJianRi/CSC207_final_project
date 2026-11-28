package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.get_note.GetNoteInputData;
import use_case.notes.get_note.GetNoteInteractor;
import use_case.notes.get_note.GetNoteOutputBoundary;
import use_case.notes.get_note.GetNoteOutputData;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GetNoteInteractorTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private RecordingPresenter presenter;
    private GetNoteInteractor interactor;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        dataAccess.save(new Note("1", "Title", "body", List.of("tag"),
                Instant.parse("2026-11-27T10:00:00Z"), Instant.parse("2026-11-27T10:00:00Z")));
        presenter = new RecordingPresenter();
        interactor = new GetNoteInteractor(dataAccess, presenter);
    }

    @Test
    void returnsTheRequestedNote() {
        interactor.execute(new GetNoteInputData("1"));

        assertNull(presenter.error);
        assertEquals("Title", presenter.output.getNote().getTitle());
        assertEquals("body", presenter.output.getNote().getMarkdown());
    }

    @Test
    void rejectsABlankId() {
        interactor.execute(new GetNoteInputData("  "));

        assertEquals("Select a note.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void reportsAnUnknownId() {
        interactor.execute(new GetNoteInputData("missing"));

        assertEquals("Note not found.", presenter.error);
        assertNull(presenter.output);
    }

    @Test
    void reportsStorageFailure() {
        GetNoteInteractor failing = new GetNoteInteractor(new FailingNoteDataAccess(), presenter);

        failing.execute(new GetNoteInputData("1"));

        assertEquals("Could not load the note. Try again.", presenter.error);
    }

    private static final class RecordingPresenter implements GetNoteOutputBoundary {
        private GetNoteOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(GetNoteOutputData output) {
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
        public Optional<Note> findById(String noteId) {
            throw new IllegalStateException("storage down");
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
