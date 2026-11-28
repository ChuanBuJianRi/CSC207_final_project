package interface_adapter.agent;

import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolRegistry;
import use_case.agent.AgentToolResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers tool executors and routes model tool calls to them.
 */
public final class AgentToolDispatcher implements AgentToolRegistry {
    private final Map<String, AgentToolExecutor> executors = new LinkedHashMap<>();

    public AgentToolDispatcher(List<AgentToolExecutor> executors) {
        for (AgentToolExecutor executor : executors) {
            String name = executor.definition().getName();
            if (this.executors.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate tool name: " + name);
            }
            this.executors.put(name, executor);
        }
    }

    @Override
    public List<AgentToolDefinition> getDefinitions() {
        List<AgentToolDefinition> definitions = new ArrayList<>();
        for (AgentToolExecutor executor : executors.values()) {
            definitions.add(executor.definition());
        }
        return List.copyOf(definitions);
    }

    @Override
    public synchronized AgentToolResult execute(AgentToolCall call) {
        if (call == null) {
            return AgentToolResult.failure("", "", "The tool call was missing.");
        }
        AgentToolExecutor executor = executors.get(call.getName());
        if (executor == null) {
            return AgentToolResult.failure(call.getId(), call.getName(),
                    "Unknown tool: " + call.getName());
        }
        try {
            AgentToolResult result = executor.execute(call);
            if (result == null) {
                return AgentToolResult.failure(call.getId(), call.getName(),
                        "The tool returned no result.");
            }
            return result;
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            return AgentToolResult.failure(call.getId(), call.getName(),
                    message == null || message.isBlank()
                            ? "The tool failed." : "The tool failed: " + message);
        }
    }
}
