package use_case.agent;

public class OpenAiResponse {
    private final boolean success;
    private final String content;
    private final String errorMessage;

    private OpenAiResponse(boolean success, String content, String errorMessage) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
    }

    public static OpenAiResponse success(String content) {
        return new OpenAiResponse(true, content, "");
    }

    public static OpenAiResponse failure(String errorMessage) {
        return new OpenAiResponse(false, "", errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
