package use_case.notes.create_note;

import entity.Note;

public final class CreateNoteOutputData {
    private final Note note;

    public CreateNoteOutputData(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }
}
