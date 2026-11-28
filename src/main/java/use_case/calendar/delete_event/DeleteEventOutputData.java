package use_case.calendar.delete_event;

public class DeleteEventOutputData {
    private final String eventId;

    public DeleteEventOutputData(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}