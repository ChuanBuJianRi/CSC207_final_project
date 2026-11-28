package use_case.agent;

import java.util.List;

/**
 * One message in the conversation sent to the model. Provider-specific replay
 * data (currently the Claude assistant content blocks) is kept opaque.
 */
public final class AgentMessage {
    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    private final Role role;
    private final String content;
    private final List<AgentToolCall> toolCalls;
    private final List<AgentToolResult> toolResults;
    private final String providerContentJson;

    private AgentMessage(Role role, String content, List<AgentToolCall> toolCalls,
                         List<AgentToolResult> toolResults, String providerContentJson) {
        this.role = role;
        this.content = content == null ? "" : content;
        this.toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
        this.toolResults = List.copyOf(toolResults == null ? List.of() : toolResults);
        this.providerContentJson = providerContentJson == null ? "" : providerContentJson;
    }

    public static AgentMessage system(String content) {
        return new AgentMessage(Role.SYSTEM, content, List.of(), List.of(), "");
    }

    public static AgentMessage user(String content) {
        return new AgentMessage(Role.USER, content, List.of(), List.of(), "");
    }

    public static AgentMessage assistant(String content, List<AgentToolCall> toolCalls,
                                         String providerContentJson) {
        return new AgentMessage(Role.ASSISTANT, content, toolCalls, List.of(), providerContentJson);
    }

    public static AgentMessage toolResults(List<AgentToolResult> results) {
        return new AgentMessage(Role.TOOL, "", List.of(), results, "");
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<AgentToolCall> getToolCalls() {
        return toolCalls;
    }

    public List<AgentToolResult> getToolResults() {
        return toolResults;
    }

    public String getProviderContentJson() {
        return providerContentJson;
    }
}
