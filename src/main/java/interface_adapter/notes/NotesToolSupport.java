package interface_adapter.notes;

import entity.Note;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared JSON parsing and formatting for the notes tool executors.
 */
public final class NotesToolSupport {
    public static final int MAX_READ_CHARS = 20_000;

    private NotesToolSupport() {
    }

    public static AgentToolDefinition definition(String name, String description,
                                                 String parametersJson) {
        return new AgentToolDefinition(name, description, parametersJson);
    }

    public static JSONObject arguments(AgentToolCall call) {
        String json = call.getArgumentsJson();
        if (json == null || json.isBlank()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(json);
        } catch (JSONException exception) {
            throw new IllegalArgumentException("The tool arguments are not valid JSON.");
        }
    }

    public static String requiredString(JSONObject arguments, String field) {
        if (!arguments.has(field) || arguments.isNull(field)) {
            throw new IllegalArgumentException("Missing required field: " + field + ".");
        }
        String value = arguments.optString(field, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + field + ".");
        }
        return value;
    }

    public static String optionalString(JSONObject arguments, String field) {
        if (!arguments.has(field) || arguments.isNull(field)) {
            return null;
        }
        return arguments.optString(field, "");
    }

    public static List<String> optionalStringList(JSONObject arguments, String field) {
        if (!arguments.has(field) || arguments.isNull(field)) {
            return null;
        }
        JSONArray array = arguments.optJSONArray(field);
        if (array == null) {
            throw new IllegalArgumentException("Field " + field + " must be an array of strings.");
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            Object value = array.opt(index);
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("Field " + field + " must be an array of strings.");
            }
            values.add((String) value);
        }
        return values;
    }

    public static int optionalInt(JSONObject arguments, String field, int fallback) {
        if (!arguments.has(field) || arguments.isNull(field)) {
            return fallback;
        }
        Object value = arguments.opt(field);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Field " + field + " must be a number.");
        }
        return ((Number) value).intValue();
    }

    public static AgentToolResult success(AgentToolCall call, JSONObject payload) {
        return AgentToolResult.success(call.getId(), call.getName(), payload.toString());
    }

    public static AgentToolResult failure(AgentToolCall call, String message) {
        return AgentToolResult.failure(call.getId(), call.getName(), message);
    }

    public static JSONObject summary(Note note) {
        return new JSONObject()
                .put("id", note.getId())
                .put("title", note.getTitle())
                .put("tags", new JSONArray(note.getTags()))
                .put("updatedAt", note.getUpdatedAt().toString());
    }

    public static JSONObject fullNote(Note note) {
        JSONObject json = summary(note)
                .put("markdown", truncate(note.getMarkdown()))
                .put("createdAt", note.getCreatedAt().toString());
        if (note.getMarkdown().length() > MAX_READ_CHARS) {
            json.put("truncated", true);
        }
        return json;
    }

    public static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_READ_CHARS) {
            return value;
        }
        return value.substring(0, MAX_READ_CHARS)
                + "\n\n[Note truncated after " + MAX_READ_CHARS + " characters.]";
    }
}
