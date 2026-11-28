package use_case.notes.list_notes;

public interface ListNotesOutputBoundary {
    void prepareSuccessView(ListNotesOutputData output);

    void prepareFailView(String errorMessage);
}
