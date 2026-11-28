package view;

/**
 * Model and effort options shared by the Agent tab and the Notes agent pane.
 */
public final class AgentModels {
    public static final String[] MODELS = {
            "gpt-6.1-sol", "gemini-3.8-flash", "claude-opus-5-5", "deepseek-chat"};
    public static final String[] EFFORTS = {"low", "medium", "high"};
    public static final String[] EFFORT_LABELS = {"Low", "Medium", "High"};

    private AgentModels() {
    }
}
