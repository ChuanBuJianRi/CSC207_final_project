package use_case.calendar.list_events;

import entity.Event;
import use_case.calendar.EventDataAccessInterface;

import java.util.List;

public final class ListEventsInteractor implements ListEventsInputBoundary {
    private final EventDataAccessInterface eventDataAccess;
    private final ListEventsOutputBoundary presenter;

    public ListEventsInteractor(EventDataAccessInterface eventDataAccess, ListEventsOutputBoundary presenter) {
        this.eventDataAccess = eventDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ListEventsInputData input) {
        List<Event> events;
        try {
            events = eventDataAccess.findAll();
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Events not loaded. Please try again.");
            return;
        }

        presenter.prepareSuccessView(new ListEventsOutputData(events));
    }
}
