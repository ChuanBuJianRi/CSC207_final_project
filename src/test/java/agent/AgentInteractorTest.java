package agent;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import entity.Memory;
import use_case.agent.AgentDataAccessInterface;
import use_case.agent.AgentInputData;
import use_case.agent.AgentInteractor;
import use_case.agent.AgentMessage;
import use_case.agent.AgentOutputBoundary;
import use_case.agent.AgentOutputData;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolRegistry;
import use_case.agent.AgentToolResult;
import use_case.agent.OpenAiGateway;
import use_case.agent.OpenAiRequest;
import use_case.agent.OpenAiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentInteractorTest {
    @Test
    void executePassesTheSelectedModelToTheGateway() {
        RecordingGateway gateway = new RecordingGateway(OpenAiResponse.success("done"));
        RecordingPresenter presenter = new RecordingPresenter();
        AgentInteractor interactor = new AgentInteractor(gateway, presenter, new RecordingDataAccess());

        interactor.execute(new AgentInputData("hello", "gpt-4.1"));

        assertEquals("gpt-4.1", gateway.lastRequest.getModel());
        assertEquals("hello", gateway.lastRequest.getUserMessage());
        assertEquals("done", presenter.success.getResponse());
        assertEquals("gpt-4.1", presenter.success.getModel());
        assertNull(presenter.failure);
    }

    @Test
    void executeAddsRelevantMemoriesAndSavesTheTurn() {
        RecordingGateway gateway = new RecordingGateway(OpenAiResponse.success("done"));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingDataAccess dataAccess = new RecordingDataAccess(List.of(new Memory("1", "exam at 9", "note")));
        AgentInteractor interactor = new AgentInteractor(gateway, presenter, dataAccess);

        interactor.execute(new AgentInputData("hello", "gpt-6.1-sol"));

        assertTrue(gateway.lastRequest.getUserMessage().contains("[note] exam at 9"));
        assertTrue(gateway.lastRequest.getUserMessage().endsWith("User message:\nhello"));
        assertEquals(2, dataAccess.saved.size());
        assertEquals("user", dataAccess.saved.get(0).getSource());
        assertEquals("assistant", dataAccess.saved.get(1).getSource());
        assertEquals("done", presenter.success.getResponse());
    }

    @Test
    void executeRejectsABlankModelWithoutCallingTheGateway() {
        RecordingGateway gateway = new RecordingGateway(OpenAiResponse.success("done"));
        RecordingPresenter presenter = new RecordingPresenter();
        AgentInteractor interactor = new AgentInteractor(gateway, presenter, new RecordingDataAccess());

        interactor.execute(new AgentInputData("hello", "  "));

        assertNull(gateway.lastRequest);
        assertEquals("Choose a model.", presenter.failure);
    }

    @Test
    void executeReportsGatewayFailure() {
        RecordingGateway gateway = new RecordingGateway(OpenAiResponse.failure("service down"));
        RecordingPresenter presenter = new RecordingPresenter();
        AgentInteractor interactor = new AgentInteractor(gateway, presenter, new RecordingDataAccess());

        interactor.execute(new AgentInputData("hello", "gpt-4o-mini"));

        assertEquals("service down", presenter.failure);
        assertNull(presenter.success);
    }

    @Test
    void executeReportsMemoryLoadFailureWithoutCallingTheModel() {
        RecordingGateway gateway = new RecordingGateway(OpenAiResponse.success("done"));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingDataAccess dataAccess = new RecordingDataAccess();
        dataAccess.failure = new IllegalStateException("VECTOR_DB_URL is not set.");
        AgentInteractor interactor = new AgentInteractor(gateway, presenter, dataAccess);

        interactor.execute(new AgentInputData("hello", "gpt-6.1-sol"));

        assertNull(gateway.lastRequest);
        assertEquals("Could not load memories: VECTOR_DB_URL is not set.", presenter.failure);
    }

    @Test
    void executesToolCallsAndSendsResultsBackToTheModel() {
        AgentToolCall call = new AgentToolCall("call_1", "create_note", "{\"title\":\"T\"}");
        SequencedGateway gateway = new SequencedGateway(List.of(
                OpenAiResponse.toolCalls("", List.of(call)),
                OpenAiResponse.success("Created the note.")));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingDataAccess dataAccess = new RecordingDataAccess();
        RecordingRegistry registry = new RecordingRegistry(List.of(
                new AgentToolDefinition("create_note", "Create a note.", "{\"type\":\"object\"}")),
                AgentToolResult.success("call_1", "create_note", "{\"id\":\"n1\"}"));
        AgentInteractor interactor = new AgentInteractor(
                gateway, presenter, dataAccess, registry);

        interactor.execute(new AgentInputData("make a note", "gpt-6.1-sol"));

        assertEquals(2, gateway.requests.size());
        assertEquals(1, registry.calls.size());
        assertEquals("Created the note.", presenter.success.getResponse());
        assertNull(presenter.failure);
        assertEquals("call_1", registry.calls.get(0).getId());
        AgentMessage toolMessage = gateway.requests.get(1).getMessages().stream()
                .filter(message -> message.getRole() == AgentMessage.Role.TOOL)
                .findFirst()
                .orElseThrow();
        assertEquals("{\"id\":\"n1\"}", toolMessage.getToolResults().get(0).getContent());
        assertEquals(2, dataAccess.saved.size());
    }

    @Test
    void passesToolDefinitionsToTheGateway() {
        SequencedGateway gateway = new SequencedGateway(List.of(OpenAiResponse.success("done")));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingRegistry registry = new RecordingRegistry(List.of(
                new AgentToolDefinition("list_notes", "List notes.", "{\"type\":\"object\"}")),
                AgentToolResult.success("", "list_notes", "{}"));
        AgentInteractor interactor = new AgentInteractor(
                gateway, presenter, new RecordingDataAccess(), registry);

        interactor.execute(new AgentInputData("hello", "gpt-6.1-sol"));

        assertEquals(1, gateway.requests.get(0).getTools().size());
        assertEquals("list_notes", gateway.requests.get(0).getTools().get(0).getName());
        assertEquals("done", presenter.success.getResponse());
    }

    @Test
    void reportsTooManyToolRounds() {
        AgentToolCall call = new AgentToolCall("call_1", "create_note", "{}");
        SequencedGateway gateway = new SequencedGateway(List.of(
                OpenAiResponse.toolCalls("", List.of(call))));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingRegistry registry = new RecordingRegistry(List.of(
                new AgentToolDefinition("create_note", "Create a note.", "{\"type\":\"object\"}")),
                AgentToolResult.success("call_1", "create_note", "{}"));
        AgentInteractor interactor = new AgentInteractor(
                gateway, presenter, new RecordingDataAccess(), registry);

        interactor.execute(new AgentInputData("loop", "gpt-6.1-sol"));

        assertEquals("The agent used too many tool steps.", presenter.failure);
        assertNull(presenter.success);
        assertEquals(6, gateway.requests.size());
    }

    @Test
    void sendsToolFailuresBackToTheModel() {
        AgentToolCall call = new AgentToolCall("call_1", "read_note", "{\"id\":\"missing\"}");
        SequencedGateway gateway = new SequencedGateway(List.of(
                OpenAiResponse.toolCalls("", List.of(call)),
                OpenAiResponse.success("No such note.")));
        RecordingPresenter presenter = new RecordingPresenter();
        RecordingRegistry registry = new RecordingRegistry(List.of(
                new AgentToolDefinition("read_note", "Read a note.", "{\"type\":\"object\"}")),
                AgentToolResult.failure("call_1", "read_note", "Note not found."));
        AgentInteractor interactor = new AgentInteractor(
                gateway, presenter, new RecordingDataAccess(), registry);

        interactor.execute(new AgentInputData("read it", "gpt-6.1-sol"));

        AgentMessage toolMessage = gateway.requests.get(1).getMessages().stream()
                .filter(message -> message.getRole() == AgentMessage.Role.TOOL)
                .findFirst()
                .orElseThrow();
        assertEquals("Note not found.", toolMessage.getToolResults().get(0).getContent());
        assertEquals("No such note.", presenter.success.getResponse());
    }

    private static final class SequencedGateway implements OpenAiGateway {
        private final List<OpenAiResponse> responses;
        private final List<OpenAiRequest> requests = new java.util.ArrayList<>();

        private SequencedGateway(List<OpenAiResponse> responses) {
            this.responses = responses;
        }

        @Override
        public OpenAiResponse complete(OpenAiRequest request) {
            requests.add(request);
            int index = Math.min(requests.size() - 1, responses.size() - 1);
            return responses.get(index);
        }
    }

    private static final class RecordingRegistry implements AgentToolRegistry {
        private final List<AgentToolDefinition> definitions;
        private final AgentToolResult result;
        private final List<AgentToolCall> calls = new java.util.ArrayList<>();

        private RecordingRegistry(List<AgentToolDefinition> definitions, AgentToolResult result) {
            this.definitions = definitions;
            this.result = result;
        }

        @Override
        public List<AgentToolDefinition> getDefinitions() {
            return definitions;
        }

        @Override
        public AgentToolResult execute(AgentToolCall call) {
            calls.add(call);
            return result;
        }
    }

    private static final class RecordingGateway implements OpenAiGateway {
        private final OpenAiResponse response;
        private OpenAiRequest lastRequest;

        private RecordingGateway(OpenAiResponse response) {
            this.response = response;
        }

        @Override
        public OpenAiResponse complete(OpenAiRequest request) {
            lastRequest = request;
            return response;
        }
    }

    private static final class RecordingPresenter implements AgentOutputBoundary {
        private AgentOutputData success;
        private String failure;

        @Override
        public void prepareSuccessView(AgentOutputData agentOutputData) {
            success = agentOutputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failure = errorMessage;
        }
    }

    private static final class RecordingDataAccess implements AgentDataAccessInterface {
        private final List<Memory> memories;
        private final List<Memory> saved = new java.util.ArrayList<>();
        private RuntimeException failure;

        private RecordingDataAccess() {
            this.memories = Collections.emptyList();
        }

        private RecordingDataAccess(List<Memory> memories) {
            this.memories = memories;
        }

        @Override
        public void save(Memory memory) {
            saved.add(memory);
        }

        @Override
        public List<Memory> findRelevant(String query) {
            if (failure != null) {
                throw failure;
            }
            return memories;
        }
    }
}
