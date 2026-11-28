package use_case.notes.update_note;

import entity.Note;

public final class UpdateNoteOutputData {
    private final Note note;

    public UpdateNoteOutputData(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }
}
