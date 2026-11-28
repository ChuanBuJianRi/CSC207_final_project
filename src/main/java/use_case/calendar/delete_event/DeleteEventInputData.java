package use_case.calendar.delete_event;

public class DeleteEventInputData {
    private final String eventId;

    public DeleteEventInputData(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}