package use_case.notes;

import entity.Note;

import java.util.List;
import java.util.Optional;

/**
 * Storage operations shared by the notes use cases.
 */
public interface NoteDataAccessInterface {
    /**
     * Stores a new note and its tags. If the id already exists, throws an
     * IllegalStateException without replacing the existing note.
     */
    void save(Note note);

    /**
     * Replaces an existing note and its tags. Returns false when the id does
     * not exist. Throws IllegalStateException on storage failure.
     */
    boolean update(Note note);

    /**
     * Deletes a note and its tags. Returns false when the id does not exist.
     * Throws IllegalStateException on storage failure.
     */
    boolean delete(String noteId);

    /**
     * Returns the note with the given id, or empty when it does not exist.
     * Throws IllegalStateException on storage failure.
     */
    Optional<Note> findById(String noteId);

    /**
     * Returns notes ordered by most recently updated first. A null or blank
     * query matches all notes; a null or blank tag disables the tag filter.
     * Throws IllegalStateException on storage failure.
     */
    List<Note> search(String query, String tag);

    /**
     * Returns all distinct tags in case-insensitive alphabetical order.
     * Throws IllegalStateException on storage failure.
     */
    List<String> findAllTags();
}
