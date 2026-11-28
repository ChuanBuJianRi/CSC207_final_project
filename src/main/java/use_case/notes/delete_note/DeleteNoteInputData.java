package use_case.notes.delete_note;

public final class DeleteNoteInputData {
    private final String noteId;

    public DeleteNoteInputData(String noteId) {
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }
}
