package interface_adapter.agent;

import use_case.agent.AgentOutputBoundary;
import use_case.agent.AgentOutputData;

public class AgentPresenter implements AgentOutputBoundary {
    private final AgentViewModel agentViewModel;

    public AgentPresenter(AgentViewModel agentViewModel) {
        this.agentViewModel = agentViewModel;
    }

    @Override
    public void prepareSuccessView(AgentOutputData agentOutputData) {
        agentViewModel.setResponse(agentOutputData.getResponse());
        agentViewModel.setModel(agentOutputData.getModel());
        agentViewModel.setErrorMessage("");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        agentViewModel.setErrorMessage(errorMessage);
    }
}
