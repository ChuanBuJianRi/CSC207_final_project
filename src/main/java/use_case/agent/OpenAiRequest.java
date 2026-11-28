package use_case.agent;

import java.util.List;

public class OpenAiRequest {
    private final String model;
    private final List<AgentMessage> messages;
    private final String effort;
    private final List<AgentToolDefinition> tools;

    public OpenAiRequest(String model, String userMessage) {
        this(model, userMessage, "");
    }

    public OpenAiRequest(String model, String userMessage, String effort) {
        this(model, List.of(AgentMessage.user(userMessage)), effort, List.of());
    }

    public OpenAiRequest(String model, List<AgentMessage> messages, String effort,
                         List<AgentToolDefinition> tools) {
        this.model = model;
        this.messages = List.copyOf(messages == null ? List.of() : messages);
        this.effort = effort;
        this.tools = List.copyOf(tools == null ? List.of() : tools);
    }

    public String getModel() {
        return model;
    }

    /**
     * Returns the last user message text, which preserves the old single-message
     * request contract for callers that still inspect it.
     */
    public String getUserMessage() {
        for (int index = messages.size() - 1; index >= 0; index--) {
            AgentMessage message = messages.get(index);
            if (message.getRole() == AgentMessage.Role.USER) {
                return message.getContent();
            }
        }
        return "";
    }

    public List<AgentMessage> getMessages() {
        return messages;
    }

    public String getEffort() {
        return effort;
    }

    public List<AgentToolDefinition> getTools() {
        return tools;
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }
}
