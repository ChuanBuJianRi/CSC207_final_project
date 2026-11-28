package use_case.notes.create_note;

public interface CreateNoteOutputBoundary {
    void prepareSuccessView(CreateNoteOutputData output);

    void prepareFailView(String errorMessage);
}
