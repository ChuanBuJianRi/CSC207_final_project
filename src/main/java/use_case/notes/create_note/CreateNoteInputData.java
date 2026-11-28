package use_case.notes.create_note;

import java.util.List;

public final class CreateNoteInputData {
    private final String title;
    private final String markdown;
    private final List<String> tags;

    public CreateNoteInputData(String title, String markdown, List<String> tags) {
        this.title = title;
        this.markdown = markdown;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdown() {
        return markdown;
    }

    public List<String> getTags() {
        return tags;
    }
}
