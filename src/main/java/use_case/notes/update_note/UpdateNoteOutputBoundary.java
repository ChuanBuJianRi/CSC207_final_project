package use_case.notes.update_note;

public interface UpdateNoteOutputBoundary {
    void prepareSuccessView(UpdateNoteOutputData output);

    void prepareFailView(String errorMessage);
}
