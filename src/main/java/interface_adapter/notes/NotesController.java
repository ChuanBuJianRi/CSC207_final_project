package interface_adapter.notes;

import use_case.notes.create_note.CreateNoteInputBoundary;
import use_case.notes.create_note.CreateNoteInputData;
import use_case.notes.delete_note.DeleteNoteInputBoundary;
import use_case.notes.delete_note.DeleteNoteInputData;
import use_case.notes.list_notes.ListNotesInputBoundary;
import use_case.notes.list_notes.ListNotesInputData;
import use_case.notes.update_note.UpdateNoteInputBoundary;
import use_case.notes.update_note.UpdateNoteInputData;

import java.util.ArrayList;
import java.util.List;

public final class NotesController {
    private final CreateNoteInputBoundary createNote;
    private final ListNotesInputBoundary listNotes;
    private final UpdateNoteInputBoundary updateNote;
    private final DeleteNoteInputBoundary deleteNote;

    public NotesController(CreateNoteInputBoundary createNote,
                           ListNotesInputBoundary listNotes,
                           UpdateNoteInputBoundary updateNote,
                           DeleteNoteInputBoundary deleteNote) {
        this.createNote = createNote;
        this.listNotes = listNotes;
        this.updateNote = updateNote;
        this.deleteNote = deleteNote;
    }

    public void loadNotes(String query, String tag) {
        listNotes.execute(new ListNotesInputData(query, tag));
    }

    public void createNote(String title, String markdown, String tagsText) {
        createNote.execute(new CreateNoteInputData(title, markdown, parseTags(tagsText)));
    }

    public void updateNote(String noteId, String title, String markdown, String tagsText) {
        updateNote.execute(new UpdateNoteInputData(
                noteId, title, markdown, parseTags(tagsText)));
    }

    public void deleteNote(String noteId) {
        deleteNote.execute(new DeleteNoteInputData(noteId));
    }

    private static List<String> parseTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String raw : tagsText.split(",")) {
            String tag = raw.trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
