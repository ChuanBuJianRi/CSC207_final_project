package use_case.agent;

import entity.Memory;

import java.util.List;
import java.util.UUID;

public class AgentInteractor implements AgentInputBoundary {
    private final OpenAiGateway openAiGateway;
    private final AgentOutputBoundary agentOutputBoundary;
    private final AgentDataAccessInterface agentDataAccessInterface;

    public AgentInteractor(OpenAiGateway openAiGateway, AgentOutputBoundary agentOutputBoundary, AgentDataAccessInterface agentDataAccessInterface) {
        this.openAiGateway = openAiGateway;
        this.agentOutputBoundary = agentOutputBoundary;
        this.agentDataAccessInterface = agentDataAccessInterface;
    }

    @Override
    public void execute(AgentInputData agentInputData) {
        String model = agentInputData.getModel() == null ? "" : agentInputData.getModel().trim();
        String userMessage = agentInputData.getUserMessage() == null ? "" : agentInputData.getUserMessage().trim();
        String effort = agentInputData.getEffort() == null ? "" : agentInputData.getEffort().trim();
        if (model.isEmpty()) {
            agentOutputBoundary.prepareFailView("Choose a model.");
            return;
        }
        if (userMessage.isEmpty()) {
            agentOutputBoundary.prepareFailView("Enter a message.");
            return;
        }
        List<Memory> memories;
        try {
            memories = agentDataAccessInterface.findRelevant(userMessage);
        } catch (RuntimeException exception) {
            agentOutputBoundary.prepareFailView("Could not load memories: " + exception.getMessage());
            return;
        }
        OpenAiResponse response = openAiGateway.complete(new OpenAiRequest(model, prompt(userMessage, memories), effort));
        if (!response.isSuccess()) {
            agentOutputBoundary.prepareFailView(response.getErrorMessage());
            return;
        }
        try {
            agentDataAccessInterface.save(new Memory(UUID.randomUUID().toString(), userMessage, "user"));
            agentDataAccessInterface.save(new Memory(UUID.randomUUID().toString(), response.getContent(), "assistant"));
        } catch (RuntimeException exception) {
            agentOutputBoundary.prepareFailView(response.getContent() + "\n\nThis reply was not saved: " + exception.getMessage());
            return;
        }
        agentOutputBoundary.prepareSuccessView(new AgentOutputData(response.getContent(), model, null));
    }

    private String prompt(String userMessage, List<Memory> memories) {
        if (memories.isEmpty()) {
            return userMessage;
        }
        StringBuilder builder = new StringBuilder("Earlier notes that may be relevant:\n");
        for (Memory memory : memories) {
            builder.append("- [").append(memory.getSource()).append("] ").append(memory.getContent()).append("\n");
        }
        builder.append("\nUser message:\n").append(userMessage);
        return builder.toString();
    }
}
