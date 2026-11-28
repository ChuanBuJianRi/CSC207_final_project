package use_case.calendar.delete_event;

public interface DeleteEventOutputBoundary {
    void prepareSuccessView(DeleteEventOutputData output);

    void prepareFailView(String errorMessage);
}
