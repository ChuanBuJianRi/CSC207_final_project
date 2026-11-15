package use_case.agent;

public class AgentInputData {
    private final String userMessage;
    private final String model;

    public AgentInputData(String userMessage, String model) {
        this.userMessage = userMessage;
        this.model = model;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getModel() {
        return model;
    }
}
