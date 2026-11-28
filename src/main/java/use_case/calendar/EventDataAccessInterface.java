package use_case.calendar;

import entity.Event;

import java.util.List;
import java.util.Optional;

/**
 * Storage operations shared by the calendar use cases.
 */
public interface EventDataAccessInterface {
    /**
     * Stores a new event.
     * Throws IllegalStateException upon storage failure or existing events.
     */
    void save(Event event);

    /**
     * Returns a snapshot of the events stored in chronological order.
     * Throws IllegalStateException upon access failure.
     */
    List<Event> findAll();
    
    /**
     * Deletes an event. Returns false upon non-existing 
     * Throws IllegalStateException on storage failure.
     */
    boolean delete(String eventId);
}