package notes;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import interface_adapter.notes.CreateNoteTool;
import interface_adapter.notes.DeleteNoteTool;
import interface_adapter.notes.ListNotesTool;
import interface_adapter.notes.NotesPresenter;
import interface_adapter.notes.NotesToolCaptures;
import interface_adapter.notes.NotesViewModel;
import interface_adapter.notes.ReadNoteTool;
import interface_adapter.notes.UpdateNoteTool;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.create_note.CreateNoteInteractor;
import use_case.notes.delete_note.DeleteNoteInteractor;
import use_case.notes.get_note.GetNoteInteractor;
import use_case.notes.list_notes.ListNotesInteractor;
import use_case.notes.update_note.UpdateNoteInteractor;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesToolsTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private NotesViewModel viewModel;
    private List<Boolean> agentEditing;
    private AgentToolExecutor createTool;
    private AgentToolExecutor listTool;
    private AgentToolExecutor readTool;
    private AgentToolExecutor updateTool;
    private AgentToolExecutor deleteTool;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        viewModel = new NotesViewModel();
        agentEditing = new ArrayList<>();
        viewModel.addPropertyChangeListener(event -> {
            if (NotesViewModel.AGENT_EDITING.equals(event.getPropertyName())) {
                agentEditing.add((Boolean) event.getNewValue());
            }
        });
        NotesPresenter presenter = new NotesPresenter(viewModel);

        NotesToolCaptures.Create createCapture = new NotesToolCaptures.Create(presenter);
        NotesToolCaptures.List listCapture = new NotesToolCaptures.List(null);
        NotesToolCaptures.Get getCapture = new NotesToolCaptures.Get();
        NotesToolCaptures.Update updateCapture = new NotesToolCaptures.Update(presenter);
        NotesToolCaptures.Delete deleteCapture = new NotesToolCaptures.Delete(presenter);

        createTool = new CreateNoteTool(
                new CreateNoteInteractor(dataAccess, createCapture), createCapture);
        listTool = new ListNotesTool(
                new ListNotesInteractor(dataAccess, listCapture), listCapture);
        readTool = new ReadNoteTool(
                new GetNoteInteractor(dataAccess, getCapture), getCapture);
        updateTool = new UpdateNoteTool(
                new UpdateNoteInteractor(dataAccess, updateCapture), updateCapture);
        deleteTool = new DeleteNoteTool(
                new DeleteNoteInteractor(dataAccess, deleteCapture), deleteCapture);
    }

    @Test
    void allToolDefinitionsHaveValidJsonSchemas() {
        for (AgentToolExecutor tool : List.of(
                listTool, readTool, createTool, updateTool, deleteTool)) {
            assertFalse(tool.definition().getName().isBlank());
            assertDoesNotThrow(() -> new JSONObject(tool.definition().getParametersJson()));
        }
    }

    @Test
    void supportsTheFullCreateListReadUpdateDeleteFlow() {
        AgentToolResult created = createTool.execute(call("create_note",
                "{\"title\":\"Algorithms\",\"markdown\":\"# Big O\\n\\nO(n log n)\","
                        + "\"tags\":[\"CSC207\",\"csc207\",\"Study\"]}"));
        assertTrue(created.isSuccess());
        String id = new JSONObject(created.getContent()).getString("id");
        assertEquals(1, viewModel.getNotes().size());
        assertEquals(List.of("CSC207", "Study"),
                dataAccess.findById(id).orElseThrow().getTags());

        AgentToolResult listed = listTool.execute(call("list_notes", "{}"));
        JSONObject listPayload = new JSONObject(listed.getContent());
        assertEquals(1, listPayload.getInt("count"));
        assertEquals(id, listPayload.getJSONArray("notes").getJSONObject(0).getString("id"));

        AgentToolResult read = readTool.execute(call("read_note", "{\"id\":\"" + id + "\"}"));
        JSONObject readPayload = new JSONObject(read.getContent());
        assertTrue(readPayload.getString("markdown").contains("O(n log n)"));

        AgentToolResult updated = updateTool.execute(call("update_note",
                "{\"id\":\"" + id + "\",\"markdown\":\"# Updated\",\"tags\":[\"Math\"]}"));
        assertTrue(updated.isSuccess());
        Note stored = dataAccess.findById(id).orElseThrow();
        assertEquals("# Updated", stored.getMarkdown());
        assertEquals(List.of("Math"), stored.getTags());
        assertEquals("Algorithms", stored.getTitle());

        AgentToolResult deleted = deleteTool.execute(call("delete_note", "{\"id\":\"" + id + "\"}"));
        assertTrue(deleted.isSuccess());
        assertTrue(dataAccess.findById(id).isEmpty());
        assertTrue(viewModel.getNotes().isEmpty());
    }

    @Test
    void listSupportsQueryTagAndLimit() {
        dataAccess.save(note("1", "Alpha", "alpha body", List.of("CSC207"),
                Instant.parse("2026-11-27T10:00:00Z")));
        dataAccess.save(note("2", "Beta", "alpha too", List.of("Personal"),
                Instant.parse("2026-11-27T09:00:00Z")));
        dataAccess.save(note("3", "Gamma", "nothing", List.of("CSC207"),
                Instant.parse("2026-11-27T08:00:00Z")));

        AgentToolResult byQuery = listTool.execute(call("list_notes", "{\"query\":\"alpha\"}"));
        assertEquals(2, new JSONObject(byQuery.getContent()).getInt("count"));

        AgentToolResult byTag = listTool.execute(call("list_notes", "{\"tag\":\"csc207\"}"));
        assertEquals(2, new JSONObject(byTag.getContent()).getInt("count"));

        AgentToolResult limited = listTool.execute(call("list_notes",
                "{\"tag\":\"csc207\",\"limit\":1}"));
        JSONObject limitedPayload = new JSONObject(limited.getContent());
        assertEquals(1, limitedPayload.getInt("count"));
        assertEquals(1, limitedPayload.getInt("more"));
    }

    @Test
    void readTruncatesVeryLongMarkdown() {
        String longBody = "x".repeat(20_100);
        dataAccess.save(new Note("1", "Long", longBody, List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));

        AgentToolResult result = readTool.execute(call("read_note", "{\"id\":\"1\"}"));

        JSONObject payload = new JSONObject(result.getContent());
        assertTrue(payload.getBoolean("truncated"));
        assertTrue(payload.getString("markdown").contains("truncated"));
        assertFalse(payload.getString("markdown").length() > 20_200);
    }

    @Test
    void readAndListToolsDoNotReplaceTheVisibleViewModel() {
        dataAccess.save(note("1", "Existing", "", List.of(),
                Instant.parse("2026-11-27T10:00:00Z")));
        AgentToolResult created = createTool.execute(call("create_note", "{\"title\":\"New\"}"));
        assertTrue(created.isSuccess());

        listTool.execute(call("list_notes", "{}"));
        readTool.execute(call("read_note", "{\"id\":\"1\"}"));

        assertEquals(1, viewModel.getNotes().size());
        assertEquals("New", viewModel.getNotes().get(0).getTitle());
    }

    @Test
    void writeToolsSignalAgentEditingAroundTheWrite() {
        createTool.execute(call("create_note", "{\"title\":\"Signal\"}"));

        assertEquals(List.of(true, false), agentEditing);
    }

    @Test
    void invalidArgumentsReturnFailures() {
        assertFalse(createTool.execute(call("create_note", "{}")).isSuccess());
        assertFalse(readTool.execute(call("read_note", "{}")).isSuccess());
        assertFalse(updateTool.execute(call("update_note", "{\"id\":\"1\"}")).isSuccess());
        assertFalse(listTool.execute(call("list_notes", "{\"limit\":\"many\"}")).isSuccess());
        assertFalse(createTool.execute(call("create_note", "not json")).isSuccess());
    }

    @Test
    void missingRequiredFieldsReturnFailures() {
        AgentToolResult result = createTool.execute(call("create_note", "{\"title\":\"  \"}"));

        assertFalse(result.isSuccess());
        assertEquals("Missing required field: title.", result.getContent());
    }

    @Test
    void interactorStorageFailuresAreReturnedToTheModel() {
        NotesViewModel failingViewModel = new NotesViewModel();
        NotesPresenter failingPresenter = new NotesPresenter(failingViewModel);
        NotesToolCaptures.Create capture = new NotesToolCaptures.Create(failingPresenter);
        CreateNoteTool failingTool = new CreateNoteTool(
                new CreateNoteInteractor(new FailingNoteDataAccess(), capture), capture);

        AgentToolResult result = failingTool.execute(call("create_note", "{\"title\":\"T\"}"));

        assertFalse(result.isSuccess());
        assertEquals("Could not save the note. Try again.", result.getContent());
        assertEquals("Could not save the note. Try again.",
                failingViewModel.getErrorMessage());
    }

    private static AgentToolCall call(String name, String arguments) {
        return new AgentToolCall("call_1", name, arguments);
    }

    private static Note note(String id, String title, String markdown, List<String> tags,
                             Instant updatedAt) {
        return new Note(id, title, markdown, tags, updatedAt, updatedAt);
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
