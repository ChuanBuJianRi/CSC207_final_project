package interface_adapter.notes;

import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.update_note.UpdateNoteInputBoundary;
import use_case.notes.update_note.UpdateNoteInputData;
import use_case.notes.update_note.UpdateNoteOutputData;

import java.util.List;

public final class UpdateNoteTool implements AgentToolExecutor {
    private static final String NAME = "update_note";
    private static final String DESCRIPTION =
            "Update an existing note by id. Omitted fields keep their current value. "
                    + "Provide at least one of title, markdown or tags.";
    private static final String PARAMETERS = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "\"id\":{\"type\":\"string\",\"description\":\"The note id.\"},"
            + "\"title\":{\"type\":\"string\",\"description\":\"The new title.\"},"
            + "\"markdown\":{\"type\":\"string\",\"description\":\"The new Markdown body.\"},"
            + "\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},"
            + "\"description\":\"The complete replacement tag list.\"}"
            + "},"
            + "\"required\":[\"id\"]"
            + "}";

    private final UpdateNoteInputBoundary interactor;
    private final NotesToolCaptures.Update capture;

    public UpdateNoteTool(UpdateNoteInputBoundary interactor, NotesToolCaptures.Update capture) {
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
            String title = NotesToolSupport.optionalString(arguments, "title");
            String markdown = NotesToolSupport.optionalString(arguments, "markdown");
            List<String> tags = NotesToolSupport.optionalStringList(arguments, "tags");
            if (title == null && markdown == null && tags == null) {
                return NotesToolSupport.failure(call,
                        "Provide a title, markdown, or tags to update.");
            }
            if (title != null) {
                title = title.trim();
            }

            capture.reset();
            capture.beginEditing();
            try {
                interactor.execute(new UpdateNoteInputData(id, title, markdown, tags));
            } finally {
                capture.endEditing();
            }
            if (capture.getError() != null) {
                return NotesToolSupport.failure(call, capture.getError());
            }
            UpdateNoteOutputData output = capture.getOutput();
            return output == null
                    ? NotesToolSupport.failure(call, "The note could not be updated.")
                    : NotesToolSupport.success(call, NotesToolSupport.summary(output.getNote()));
        } catch (IllegalArgumentException exception) {
            return NotesToolSupport.failure(call, exception.getMessage());
        }
    }
}
