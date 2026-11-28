package use_case.calendar.create_event;

import entity.Event;

public class CreateEventOutputData {
    private final Event event;

    public CreateEventOutputData(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}
