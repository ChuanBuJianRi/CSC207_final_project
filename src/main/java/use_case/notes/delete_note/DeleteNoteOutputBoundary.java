package use_case.notes.delete_note;

public interface DeleteNoteOutputBoundary {
    void prepareSuccessView(DeleteNoteOutputData output);

    void prepareFailView(String errorMessage);
}
