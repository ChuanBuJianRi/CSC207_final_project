package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.create_note.CreateNoteInputData;
import use_case.notes.create_note.CreateNoteInteractor;
import use_case.notes.create_note.CreateNoteOutputBoundary;
import use_case.notes.create_note.CreateNoteOutputData;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateNoteInteractorTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private RecordingPresenter presenter;
    private CreateNoteInteractor interactor;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        presenter = new RecordingPresenter();
        interactor = new CreateNoteInteractor(dataAccess, presenter);
    }

    @Test
    void createsATrimmedNoteWithNormalizedTags() {
        interactor.execute(new CreateNoteInputData(
                "  Exam prep  ", "# Heading", List.of(" CSC207 ", "csc207", "", "Study")));

        assertNull(presenter.error);
        Note note = presenter.output.getNote();
        assertEquals("Exam prep", note.getTitle());
        assertEquals("# Heading", note.getMarkdown());
        assertEquals(List.of("CSC207", "Study"), note.getTags());
        assertNotNull(note.getCreatedAt());
        assertEquals(note.getCreatedAt(), note.getUpdatedAt());
        assertNotNull(UUID.fromString(note.getId()));
        assertTrue(dataAccess.findById(note.getId()).isPresent());
    }

    @Test
    void capsTagsAtTwenty() {
        List<String> tags = java.util.stream.IntStream.range(0, 25)
                .mapToObj(index -> "tag" + index)
                .collect(java.util.stream.Collectors.toList());

        interactor.execute(new CreateNoteInputData("Title", "", tags));

        assertEquals(20, presenter.output.getNote().getTags().size());
    }

    @Test
    void rejectsABlankTitleWithoutSaving() {
        interactor.execute(new CreateNoteInputData("   ", "body", List.of()));

        assertEquals("Enter a note title.", presenter.error);
        assertNull(presenter.output);
        assertTrue(dataAccess.search("", "").isEmpty());
    }

    @Test
    void reportsStorageFailure() {
        CreateNoteInteractor failing = new CreateNoteInteractor(new FailingNoteDataAccess(), presenter);

        failing.execute(new CreateNoteInputData("Title", "", List.of()));

        assertEquals("Could not save the note. Try again.", presenter.error);
        assertNull(presenter.output);
    }

    private static final class RecordingPresenter implements CreateNoteOutputBoundary {
        private CreateNoteOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(CreateNoteOutputData output) {
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
            throw new IllegalStateException("storage down");
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
