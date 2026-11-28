package use_case.calendar.create_event;

public interface CreateEventOutputBoundary {
    void prepareSuccessView(CreateEventOutputData output);

    void prepareFailView(String errorMessage);
}