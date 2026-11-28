package use_case.notes.update_note;

import java.util.List;

/**
 * A null title, markdown or tags list means "keep the existing value".
 */
public final class UpdateNoteInputData {
    private final String noteId;
    private final String title;
    private final String markdown;
    private final List<String> tags;

    public UpdateNoteInputData(String noteId, String title, String markdown, List<String> tags) {
        this.noteId = noteId;
        this.title = title;
        this.markdown = markdown;
        this.tags = tags;
    }

    public String getNoteId() {
        return noteId;
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
