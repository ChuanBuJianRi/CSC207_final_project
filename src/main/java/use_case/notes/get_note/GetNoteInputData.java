package use_case.notes.get_note;

public final class GetNoteInputData {
    private final String noteId;

    public GetNoteInputData(String noteId) {
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }
}
