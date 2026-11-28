package interface_adapter.notes;

import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.get_note.GetNoteInputBoundary;
import use_case.notes.get_note.GetNoteInputData;
import use_case.notes.get_note.GetNoteOutputData;

public final class ReadNoteTool implements AgentToolExecutor {
    private static final String NAME = "read_note";
    private static final String DESCRIPTION =
            "Read one note by id. Returns its title, tags, timestamps and full Markdown content.";
    private static final String PARAMETERS = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "\"id\":{\"type\":\"string\",\"description\":\"The note id returned by list_notes.\"}"
            + "},"
            + "\"required\":[\"id\"]"
            + "}";

    private final GetNoteInputBoundary interactor;
    private final NotesToolCaptures.Get capture;

    public ReadNoteTool(GetNoteInputBoundary interactor, NotesToolCaptures.Get capture) {
        this.interactor = interactor;
        this.capture = capture;
    }

    @Override
    public AgentToolDefinition definition() {
        return NotesToolSupport.definition(NAME, DESCRIPTION, PARAMETERS);
    }

    @Override
    public AgentToolResult execute(AgentToolCall call) {
        try {
            JSONObject arguments = NotesToolSupport.arguments(call);
            String id = NotesToolSupport.requiredString(arguments, "id");

            capture.reset();
            interactor.execute(new GetNoteInputData(id));
            if (capture.getError() != null) {
                return NotesToolSupport.failure(call, capture.getError());
            }
            GetNoteOutputData output = capture.getOutput();
            return output == null
                    ? NotesToolSupport.failure(call, "The note could not be loaded.")
                    : NotesToolSupport.success(call, NotesToolSupport.fullNote(output.getNote()));
        } catch (IllegalArgumentException exception) {
            return NotesToolSupport.failure(call, exception.getMessage());
        }
    }
}
