package use_case.calendar.list_events;

import entity.Event;

import java.util.List;

public class ListEventsOutputData {
    private final List<Event> events;

    public ListEventsOutputData(List<Event> events) {
        this.events = List.copyOf(events);
    }

    public List<Event> getEvents() {
        return events;
    }
}