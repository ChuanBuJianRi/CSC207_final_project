package use_case.agent;

public class AgentOutputData {
    private final String response;
    private final String model;
    private final String errorMessage;

    public AgentOutputData(String response, String model, String errorMessage) {
        this.response = response;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return errorMessage == null || errorMessage.isBlank();
    }

    public String getResponse() {
        return response;
    }

    public String getModel() {
        return model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
