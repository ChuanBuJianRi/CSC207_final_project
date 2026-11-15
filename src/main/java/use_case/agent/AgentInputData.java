package use_case.agent;

public class AgentInputData {
    private final String userMessage;
    private final String model;
    private final String effort;

    public AgentInputData(String userMessage, String model) {
        this(userMessage, model, "");
    }

    public AgentInputData(String userMessage, String model, String effort) {
        this.userMessage = userMessage;
        this.model = model;
        this.effort = effort;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getModel() {
        return model;
    }

    public String getEffort() {
        return effort;
    }
}
