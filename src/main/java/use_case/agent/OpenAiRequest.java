package use_case.agent;

public class OpenAiRequest {
    private final String model;
    private final String userMessage;

    public OpenAiRequest(String model, String userMessage) {
        this.model = model;
        this.userMessage = userMessage;
    }

    public String getModel() {
        return model;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
