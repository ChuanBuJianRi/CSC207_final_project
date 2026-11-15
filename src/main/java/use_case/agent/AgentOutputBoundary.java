package use_case.agent;

public interface AgentOutputBoundary {
    void prepareSuccessView(AgentOutputData agentOutputData);

    void prepareFailView(String errorMessage);
}
