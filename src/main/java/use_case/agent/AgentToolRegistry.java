package use_case.agent;

import java.util.List;

/**
 * Provides the available tools and dispatches a call by name.
 */
public interface AgentToolRegistry {
    List<AgentToolDefinition> getDefinitions();

    AgentToolResult execute(AgentToolCall call);
}
