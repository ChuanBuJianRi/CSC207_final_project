package agent;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import entity.Memory;
import use_case.agent.AgentDataAccessInterface;
import use_case.agent.AgentInputData;
import use_case.agent.AgentInteractor;
import use_case.agent.AgentOutputBoundary;
import use_case.agent.AgentOutputData;
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
