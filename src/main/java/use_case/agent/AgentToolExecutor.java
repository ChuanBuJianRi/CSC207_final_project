package use_case.agent;

/**
 * One named tool the agent can call.
 */
public interface AgentToolExecutor {
    AgentToolDefinition definition();

    AgentToolResult execute(AgentToolCall call);
}
