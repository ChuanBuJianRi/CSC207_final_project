package use_case.agent;

public class OpenAiRequest {
    private final String model;
    private final String userMessage;
    private final String effort;

    public OpenAiRequest(String model, String userMessage) {
        this(model, userMessage, "");
    }

    public OpenAiRequest(String model, String userMessage, String effort) {
        this.model = model;
        this.userMessage = userMessage;
        this.effort = effort;
    }

    public String getModel() {
        return model;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getEffort() {
        return effort;
    }
}
