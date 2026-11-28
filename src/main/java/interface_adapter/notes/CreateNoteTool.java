package interface_adapter.notes;

import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.create_note.CreateNoteInputBoundary;
import use_case.notes.create_note.CreateNoteInputData;
import use_case.notes.create_note.CreateNoteOutputData;

import java.util.List;

public final class CreateNoteTool implements AgentToolExecutor {
    private static final String NAME = "create_note";
    private static final String DESCRIPTION =
            "Create a new Markdown note with a title, optional Markdown body and optional tags. "
                    + "Returns the new note id.";
    private static final String PARAMETERS = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "\"title\":{\"type\":\"string\",\"description\":\"The note title.\"},"
            + "\"markdown\":{\"type\":\"string\",\"description\":\"The Markdown body.\"},"
            + "\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},"
            + "\"description\":\"Tags for this note.\"}"
            + "},"
            + "\"required\":[\"title\"]"
            + "}";

    private final CreateNoteInputBoundary interactor;
    private final NotesToolCaptures.Create capture;

    public CreateNoteTool(CreateNoteInputBoundary interactor, NotesToolCaptures.Create capture) {
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
            String title = NotesToolSupport.requiredString(arguments, "title");
            String markdown = NotesToolSupport.optionalString(arguments, "markdown");
            List<String> tags = NotesToolSupport.optionalStringList(arguments, "tags");

            capture.reset();
            capture.beginEditing();
            try {
                interactor.execute(new CreateNoteInputData(
                        title, markdown == null ? "" : markdown,
                        tags == null ? List.of() : tags));
            } finally {
                capture.endEditing();
            }
            if (capture.getError() != null) {
                return NotesToolSupport.failure(call, capture.getError());
            }
            CreateNoteOutputData output = capture.getOutput();
            return output == null
                    ? NotesToolSupport.failure(call, "The note could not be created.")
                    : NotesToolSupport.success(call, NotesToolSupport.summary(output.getNote()));
        } catch (IllegalArgumentException exception) {
            return NotesToolSupport.failure(call, exception.getMessage());
        }
    }
}
