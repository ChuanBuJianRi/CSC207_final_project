package interface_adapter.agent;

import use_case.agent.AgentInputBoundary;
import use_case.agent.AgentInputData;

public class AgentController {
    private final AgentInputBoundary agentInputBoundary;

    public AgentController(AgentInputBoundary agentInputBoundary) {
        this.agentInputBoundary = agentInputBoundary;
    }

    public void execute(String userMessage, String model, String effort) {
        agentInputBoundary.execute(new AgentInputData(userMessage, model, effort));
    }
}
