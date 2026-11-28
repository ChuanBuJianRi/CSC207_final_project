package interface_adapter.notes;

import entity.Note;
import org.json.JSONArray;
import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;
import use_case.notes.list_notes.ListNotesInputBoundary;
import use_case.notes.list_notes.ListNotesInputData;
import use_case.notes.list_notes.ListNotesOutputData;

import java.util.List;

public final class ListNotesTool implements AgentToolExecutor {
    private static final String NAME = "list_notes";
    private static final String DESCRIPTION =
            "List or search the user's notes. Returns note ids, titles, tags and update times. "
                    + "Use the query field to search titles and markdown, and the tag field to filter.";
    private static final String PARAMETERS = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "\"query\":{\"type\":\"string\",\"description\":\"Text to match in titles or markdown.\"},"
            + "\"tag\":{\"type\":\"string\",\"description\":\"Only return notes with this tag.\"},"
            + "\"limit\":{\"type\":\"integer\",\"description\":\"Maximum notes to return (1-50, default 20).\"}"
            + "}"
            + "}";

    private final ListNotesInputBoundary interactor;
    private final NotesToolCaptures.List capture;

    public ListNotesTool(ListNotesInputBoundary interactor, NotesToolCaptures.List capture) {
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
            String query = NotesToolSupport.optionalString(arguments, "query");
            String tag = NotesToolSupport.optionalString(arguments, "tag");
            int limit = NotesToolSupport.optionalInt(arguments, "limit", 20);
            limit = Math.max(1, Math.min(50, limit));

            capture.reset();
            interactor.execute(new ListNotesInputData(query, tag));
            if (capture.getError() != null) {
                return NotesToolSupport.failure(call, capture.getError());
            }
            ListNotesOutputData output = capture.getOutput();
            if (output == null) {
                return NotesToolSupport.failure(call, "The note list could not be loaded.");
            }
            List<Note> notes = output.getNotes();
            JSONArray summaries = new JSONArray();
            int returned = Math.min(notes.size(), limit);
            for (int index = 0; index < returned; index++) {
                summaries.put(NotesToolSupport.summary(notes.get(index)));
            }
            JSONObject payload = new JSONObject()
                    .put("notes", summaries)
                    .put("count", returned);
            if (notes.size() > returned) {
                payload.put("more", notes.size() - returned);
            }
            return NotesToolSupport.success(call, payload);
        } catch (IllegalArgumentException exception) {
            return NotesToolSupport.failure(call, exception.getMessage());
        }
    }
}
