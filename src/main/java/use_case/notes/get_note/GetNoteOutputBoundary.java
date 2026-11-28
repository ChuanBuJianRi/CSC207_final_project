package use_case.notes.get_note;

public interface GetNoteOutputBoundary {
    void prepareSuccessView(GetNoteOutputData output);

    void prepareFailView(String errorMessage);
}
