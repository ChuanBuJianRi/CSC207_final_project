package use_case.calendar.create_event;

import entity.Event;
import use_case.calendar.EventDataAccessInterface;

import java.util.UUID;

public final class CreateEventInteractor implements CreateEventInputBoundary {
    private final EventDataAccessInterface eventDataAccess;
    private final CreateEventOutputBoundary presenter;

    public CreateEventInteractor(EventDataAccessInterface eventDataAccess, CreateEventOutputBoundary presenter) {
        this.eventDataAccess = eventDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(CreateEventInputData input) {
        String title = input.getTitle().trim();
        String time = input.getTime();
        if (title == null || title == "") {
            presenter.prepareFailView("Enter the title of your new event:");
            return;
        }

        Event event = new Event(
            UUID.randomUUID().toString(),
            title,
            time
        );

        try {
            eventDataAccess.save(event);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Event not saved. Please try again.");
            return;
        }
        presenter.prepareSuccessView(new CreateEventOutputData(event));
    }
}
