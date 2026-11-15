package app;

import data_access.OpenaiClient;
import data_access.VectorDataAccess;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentPresenter;
import interface_adapter.agent.AgentViewModel;
import use_case.agent.AgentInteractor;
import view.AgentPanel;
import view.MainFrame;

public class AppBuilder {
    public MainFrame build() {
        AgentViewModel agentViewModel = new AgentViewModel();
        AgentPresenter agentPresenter = new AgentPresenter(agentViewModel);
        AgentInteractor agentInteractor = new AgentInteractor(
                OpenaiClient.fromEnvironment(),
                agentPresenter,
                VectorDataAccess.fromEnvironment());
        AgentController agentController = new AgentController(agentInteractor);
        AgentPanel agentPanel = new AgentPanel(agentController, agentViewModel);
        return new MainFrame(agentPanel);
    }
}
