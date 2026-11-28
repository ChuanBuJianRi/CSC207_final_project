package agent;

import interface_adapter.agent.AgentToolDispatcher;
import org.junit.jupiter.api.Test;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolExecutor;
import use_case.agent.AgentToolResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolDispatcherTest {
    @Test
    void listsDefinitionsInRegistrationOrder() {
        AgentToolDispatcher dispatcher = new AgentToolDispatcher(List.of(
                new FakeTool("one"), new FakeTool("two")));

        assertEquals("one", dispatcher.getDefinitions().get(0).getName());
        assertEquals("two", dispatcher.getDefinitions().get(1).getName());
    }

    @Test
    void routesCallsByName() {
        AgentToolDispatcher dispatcher = new AgentToolDispatcher(List.of(new FakeTool("echo")));

        AgentToolResult result = dispatcher.execute(new AgentToolCall("1", "echo", "payload"));

        assertTrue(result.isSuccess());
        assertEquals("payload", result.getContent());
    }

    @Test
    void returnsFailureForAnUnknownTool() {
        AgentToolDispatcher dispatcher = new AgentToolDispatcher(List.of());

        AgentToolResult result = dispatcher.execute(new AgentToolCall("1", "missing", "{}"));

        assertFalse(result.isSuccess());
        assertEquals("Unknown tool: missing", result.getContent());
    }

    @Test
    void catchesExecutorFailures() {
        AgentToolDispatcher dispatcher = new AgentToolDispatcher(List.of(
                new AgentToolExecutor() {
                    @Override
                    public AgentToolDefinition definition() {
                        return new AgentToolDefinition("boom", "", "{}");
                    }

                    @Override
                    public AgentToolResult execute(AgentToolCall call) {
                        throw new IllegalStateException("kaboom");
                    }
                }));

        AgentToolResult result = dispatcher.execute(new AgentToolCall("1", "boom", "{}"));

        assertFalse(result.isSuccess());
        assertTrue(result.getContent().contains("kaboom"));
    }

    @Test
    void rejectsDuplicateToolNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgentToolDispatcher(List.of(new FakeTool("same"), new FakeTool("same"))));
    }

    private static final class FakeTool implements AgentToolExecutor {
        private final String name;

        private FakeTool(String name) {
            this.name = name;
        }

        @Override
        public AgentToolDefinition definition() {
            return new AgentToolDefinition(name, "description", "{\"type\":\"object\"}");
        }

        @Override
        public AgentToolResult execute(AgentToolCall call) {
            return AgentToolResult.success(call.getId(), call.getName(), call.getArgumentsJson());
        }
    }
}
