package use_case.notes.list_notes;

import entity.Note;

import java.util.List;

public final class ListNotesOutputData {
    private final List<Note> notes;
    private final List<String> tags;

    public ListNotesOutputData(List<Note> notes, List<String> tags) {
        this.notes = List.copyOf(notes);
        this.tags = List.copyOf(tags);
    }

    public List<Note> getNotes() {
        return notes;
    }

    public List<String> getTags() {
        return tags;
    }
}
