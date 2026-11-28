package interface_adapter.notes;

import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.delete_note.DeleteNoteInputBoundary;
import use_case.notes.delete_note.DeleteNoteInputData;
import use_case.notes.delete_note.DeleteNoteOutputData;

public final class DeleteNoteTool implements AgentToolExecutor {
    private static final String NAME = "delete_note";
    private static final String DESCRIPTION =
            "Permanently delete a note by id. This cannot be undone. Only call this tool when the "
                    + "user explicitly asks to delete a note.";
    private static final String PARAMETERS = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "\"id\":{\"type\":\"string\",\"description\":\"The id of the note to delete.\"}"
            + "},"
            + "\"required\":[\"id\"]"
            + "}";

    private final DeleteNoteInputBoundary interactor;
    private final NotesToolCaptures.Delete capture;

    public DeleteNoteTool(DeleteNoteInputBoundary interactor, NotesToolCaptures.Delete capture) {
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
            capture.beginEditing();
            try {
                interactor.execute(new DeleteNoteInputData(id));
            } finally {
                capture.endEditing();
            }
            if (capture.getError() != null) {
                return NotesToolSupport.failure(call, capture.getError());
            }
            DeleteNoteOutputData output = capture.getOutput();
            if (output == null) {
                return NotesToolSupport.failure(call, "The note could not be deleted.");
            }
            return NotesToolSupport.success(call, new JSONObject()
                    .put("deleted", true)
                    .put("id", output.getNoteId()));
        } catch (IllegalArgumentException exception) {
            return NotesToolSupport.failure(call, exception.getMessage());
        }
    }
}
