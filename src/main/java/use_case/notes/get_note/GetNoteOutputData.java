package use_case.notes.get_note;

import entity.Note;

public final class GetNoteOutputData {
    private final Note note;

    public GetNoteOutputData(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }
}
