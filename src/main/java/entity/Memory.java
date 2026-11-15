package entity;

public class Memory {
    private final String id;
    private final String content;
    private final String source;

    public Memory(String id, String content, String source) {
        this.id = id;
        this.content = content;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }
}
