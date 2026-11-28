package entity;

import java.time.MonthDay;

/**
 * An event
 */
public final class Event {
    private final String id;
    private final String title;
    private final String time;

    public Event(String id, String title, String time) {
        this.id = id;
        this.title = title;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }
}
