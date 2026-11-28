package use_case.calendar.list_events;

public interface ListEventsOutputBoundary {
    void prepareSuccessView(ListEventsOutputData output);

    void prepareFailView(String errorMessage);
}
