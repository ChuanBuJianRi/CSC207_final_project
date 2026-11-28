package use_case.agent;

/**
 * The outcome of one tool invocation, sent back to the model.
 */
public final class AgentToolResult {
    private final String callId;
    private final String name;
    private final boolean success;
    private final String content;

    private AgentToolResult(String callId, String name, boolean success, String content) {
        this.callId = callId;
        this.name = name;
        this.success = success;
        this.content = content == null ? "" : content;
    }

    public static AgentToolResult success(String callId, String name, String content) {
        return new AgentToolResult(callId, name, true, content);
    }

    public static AgentToolResult failure(String callId, String name, String errorMessage) {
        return new AgentToolResult(callId, name, false, errorMessage);
    }

    public String getCallId() {
        return callId;
    }

    public String getName() {
        return name;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }
}
