package use_case.calendar.create_event;

public final class CreateEventInputData {
    private final String title;
    private final String time;

    public CreateEventInputData(String title, String time) {
        this.title = title;
        this.time = time;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getTime() {
        return time;
    }
}