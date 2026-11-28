package use_case.agent;

/**
 * One tool invocation requested by the model.
 */
public final class AgentToolCall {
    private final String id;
    private final String name;
    private final String argumentsJson;

    public AgentToolCall(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }
}
