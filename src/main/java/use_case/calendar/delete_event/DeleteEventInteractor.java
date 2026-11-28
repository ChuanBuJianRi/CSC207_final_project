package use_case.calendar.delete_event;

import use_case.calendar.EventDataAccessInterface;

public final class DeleteEventInteractor implements DeleteEventInputBoundary {
    private final EventDataAccessInterface eventDataAccess;
    private final DeleteEventOutputBoundary presenter;

    public DeleteEventInteractor(EventDataAccessInterface eventDataAccess,
                                DeleteEventOutputBoundary presenter) {
        this.eventDataAccess = eventDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(DeleteEventInputData input) {
        String eventId = input.getEventId();
        if (eventId == "") {
            presenter.prepareFailView("Please pick an event.");
            return;
        }
        eventId = eventId.trim();
        try {
            if (!eventDataAccess.delete(eventId)) {
                presenter.prepareFailView("Event not found.");
                return;
            }
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Deletion failed. Please try again.");
            return;
        }
        presenter.prepareSuccessView(new DeleteEventOutputData(eventId));
    }
}