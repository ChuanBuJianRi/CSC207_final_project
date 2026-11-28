package use_case.agent;

/**
 * A provider-neutral tool description. The parameters are a JSON Schema
 * object stored as text so the use-case layer does not depend on a JSON library.
 */
public final class AgentToolDefinition {
    private final String name;
    private final String description;
    private final String parametersJson;

    public AgentToolDefinition(String name, String description, String parametersJson) {
        this.name = name;
        this.description = description;
        this.parametersJson = parametersJson;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParametersJson() {
        return parametersJson;
    }
}
