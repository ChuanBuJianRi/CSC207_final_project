package interface_adapter.notes;

import entity.Note;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public final class NotesViewModel {
    public static final String NOTES_LOADED = "notesLoaded";
    public static final String NOTE_SAVED = "noteSaved";
    public static final String NOTE_DELETED = "noteDeleted";
    public static final String AGENT_EDITING = "agentEditing";
    public static final String ERROR = "error";

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final List<Note> notes = new ArrayList<>();
    private List<String> tags = List.of();
    private String errorMessage = "";

    public List<Note> getNotes() {
        synchronized (this) {
            return List.copyOf(notes);
        }
    }

    public List<String> getTags() {
        synchronized (this) {
            return List.copyOf(tags);
        }
    }

    public String getErrorMessage() {
        synchronized (this) {
            return errorMessage;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    void replaceNotes(List<Note> loadedNotes, List<String> loadedTags) {
        synchronized (this) {
            notes.clear();
            notes.addAll(loadedNotes);
            tags = List.copyOf(loadedTags);
            errorMessage = "";
        }
        changes.firePropertyChange(NOTES_LOADED, null, loadedNotes);
    }

    void noteSaved(Note note) {
        synchronized (this) {
            boolean replaced = false;
            for (int index = 0; index < notes.size(); index++) {
                if (notes.get(index).getId().equals(note.getId())) {
                    notes.set(index, note);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                notes.add(note);
            }
            errorMessage = "";
        }
        changes.firePropertyChange(NOTE_SAVED, null, note);
    }

    void noteDeleted(String noteId) {
        synchronized (this) {
            notes.removeIf(note -> note.getId().equals(noteId));
            errorMessage = "";
        }
        changes.firePropertyChange(NOTE_DELETED, null, noteId);
    }

    void setAgentEditing(boolean editing) {
        changes.firePropertyChange(AGENT_EDITING, null, editing);
    }

    void showError(String message) {
        synchronized (this) {
            errorMessage = message == null ? "" : message;
        }
        changes.firePropertyChange(ERROR, null, errorMessage);
    }
}
