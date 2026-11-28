package use_case.agent;

import entity.Memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentInteractor implements AgentInputBoundary {
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final String SYSTEM_PROMPT =
            "You are a study assistant with direct access to the user's Markdown notes. "
                    + "Use the notes tools to list, search, read, create, update and delete notes "
                    + "when the user asks. Always use a tool result as confirmation before claiming "
                    + "that a note changed; never invent note ids. Deleting a note is permanent, "
                    + "so only do it when the user explicitly asks.";

    private static final AgentToolRegistry NO_TOOLS = new AgentToolRegistry() {
        @Override
        public List<AgentToolDefinition> getDefinitions() {
            return List.of();
        }

        @Override
        public AgentToolResult execute(AgentToolCall call) {
            return AgentToolResult.failure(call.getId(), call.getName(), "No tools are available.");
        }
    };

    private final OpenAiGateway openAiGateway;
    private final AgentOutputBoundary agentOutputBoundary;
    private final AgentDataAccessInterface agentDataAccessInterface;
    private final AgentToolRegistry toolRegistry;

    public AgentInteractor(OpenAiGateway openAiGateway, AgentOutputBoundary agentOutputBoundary,
                           AgentDataAccessInterface agentDataAccessInterface) {
        this(openAiGateway, agentOutputBoundary, agentDataAccessInterface, NO_TOOLS);
    }

    public AgentInteractor(OpenAiGateway openAiGateway, AgentOutputBoundary agentOutputBoundary,
                           AgentDataAccessInterface agentDataAccessInterface,
                           AgentToolRegistry toolRegistry) {
        this.openAiGateway = openAiGateway;
        this.agentOutputBoundary = agentOutputBoundary;
        this.agentDataAccessInterface = agentDataAccessInterface;
        this.toolRegistry = toolRegistry == null ? NO_TOOLS : toolRegistry;
    }

    @Override
    public void execute(AgentInputData agentInputData) {
        String model = agentInputData.getModel() == null ? "" : agentInputData.getModel().trim();
        String userMessage = agentInputData.getUserMessage() == null
                ? "" : agentInputData.getUserMessage().trim();
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

        List<AgentMessage> messages = new ArrayList<>();
        messages.add(AgentMessage.system(SYSTEM_PROMPT));
        messages.add(AgentMessage.user(prompt(userMessage, memories)));
        List<AgentToolDefinition> tools = toolRegistry.getDefinitions();

        String finalContent;
        int toolRounds = 0;
        while (true) {
            OpenAiResponse response = openAiGateway.complete(
                    new OpenAiRequest(model, messages, effort, tools));
            if (!response.isSuccess()) {
                agentOutputBoundary.prepareFailView(response.getErrorMessage());
                return;
            }
            if (!response.hasToolCalls()) {
                finalContent = response.getContent();
                break;
            }
            if (toolRounds >= MAX_TOOL_ROUNDS) {
                agentOutputBoundary.prepareFailView("The agent used too many tool steps.");
                return;
            }
            toolRounds++;
            messages.add(AgentMessage.assistant(
                    response.getContent(),
                    response.getToolCalls(),
                    response.getRawAssistantContentJson()));
            List<AgentToolResult> results = new ArrayList<>();
            for (AgentToolCall call : response.getToolCalls()) {
                results.add(toolRegistry.execute(call));
            }
            messages.add(AgentMessage.toolResults(results));
        }
        if (finalContent == null || finalContent.isBlank()) {
            if (toolRounds > 0) {
                finalContent = "Done.";
            } else {
                agentOutputBoundary.prepareFailView("The model returned no text.");
                return;
            }
        }

        try {
            agentDataAccessInterface.save(new Memory(
                    UUID.randomUUID().toString(), userMessage, "user"));
            agentDataAccessInterface.save(new Memory(
                    UUID.randomUUID().toString(), finalContent, "assistant"));
        } catch (RuntimeException exception) {
            agentOutputBoundary.prepareFailView(finalContent
                    + "\n\nThis reply was not saved: " + exception.getMessage());
            return;
        }
        agentOutputBoundary.prepareSuccessView(
                new AgentOutputData(finalContent, model, null));
    }

    private String prompt(String userMessage, List<Memory> memories) {
        if (memories.isEmpty()) {
            return userMessage;
        }
        StringBuilder builder = new StringBuilder("Earlier notes that may be relevant:\n");
        for (Memory memory : memories) {
            builder.append("- [").append(memory.getSource()).append("] ")
                    .append(memory.getContent()).append("\n");
        }
        builder.append("\nUser message:\n").append(userMessage);
        return builder.toString();
    }
}
