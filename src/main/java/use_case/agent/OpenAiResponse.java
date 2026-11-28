package use_case.agent;

import java.util.List;

public class OpenAiResponse {
    private final boolean success;
    private final String content;
    private final String errorMessage;
    private final List<AgentToolCall> toolCalls;
    private final String rawAssistantContentJson;

    private OpenAiResponse(boolean success, String content, String errorMessage,
                           List<AgentToolCall> toolCalls, String rawAssistantContentJson) {
        this.success = success;
        this.content = content == null ? "" : content;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        this.toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
        this.rawAssistantContentJson = rawAssistantContentJson == null
                ? "" : rawAssistantContentJson;
    }

    public static OpenAiResponse success(String content) {
        return new OpenAiResponse(true, content, "", List.of(), "");
    }

    public static OpenAiResponse failure(String errorMessage) {
        return new OpenAiResponse(false, "", errorMessage, List.of(), "");
    }

    public static OpenAiResponse toolCalls(String content, List<AgentToolCall> toolCalls) {
        return toolCalls(content, toolCalls, "");
    }

    public static OpenAiResponse toolCalls(String content, List<AgentToolCall> toolCalls,
                                           String rawAssistantContentJson) {
        return new OpenAiResponse(true, content, "", toolCalls, rawAssistantContentJson);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<AgentToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public String getRawAssistantContentJson() {
        return rawAssistantContentJson;
    }
}
