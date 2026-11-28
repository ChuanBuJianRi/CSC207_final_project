package use_case.notes.delete_note;

public final class DeleteNoteOutputData {
    private final String noteId;

    public DeleteNoteOutputData(String noteId) {
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }
}
