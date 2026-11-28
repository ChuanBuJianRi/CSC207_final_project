package interface_adapter.notes;

import entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import use_case.notes.create_note.CreateNoteOutputData;
import use_case.notes.delete_note.DeleteNoteOutputData;
import use_case.notes.list_notes.ListNotesOutputData;
import use_case.notes.update_note.UpdateNoteOutputData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesPresenterTest {
    private NotesViewModel viewModel;
    private NotesPresenter presenter;
    private List<String> events;

    @BeforeEach
    void setUp() {
        viewModel = new NotesViewModel();
        presenter = new NotesPresenter(viewModel);
        events = new ArrayList<>();
        viewModel.addPropertyChangeListener(event -> events.add(event.getPropertyName()));
    }

    @Test
    void createAddsTheNoteAndFiresSaved() {
        Note note = note("1", "Title");

        presenter.prepareSuccessView(new CreateNoteOutputData(note));

        assertEquals(List.of(note), viewModel.getNotes());
        assertEquals(List.of(NotesViewModel.NOTE_SAVED), events);
    }

    @Test
    void listReplacesNotesAndTags() {
        Note note = note("1", "Title");

        presenter.prepareSuccessView(new ListNotesOutputData(
                List.of(note), List.of("CSC207", "Study")));

        assertEquals(List.of(note), viewModel.getNotes());
        assertEquals(List.of("CSC207", "Study"), viewModel.getTags());
        assertEquals(List.of(NotesViewModel.NOTES_LOADED), events);
    }

    @Test
    void updateReplacesTheExistingNote() {
        presenter.prepareSuccessView(new CreateNoteOutputData(note("1", "Old")));
        Note updated = note("1", "New");

        presenter.prepareSuccessView(new UpdateNoteOutputData(updated));

        assertEquals(1, viewModel.getNotes().size());
        assertEquals("New", viewModel.getNotes().get(0).getTitle());
        assertEquals(NotesViewModel.NOTE_SAVED, events.get(events.size() - 1));
    }

    @Test
    void deleteRemovesTheNote() {
        presenter.prepareSuccessView(new CreateNoteOutputData(note("1", "Title")));

        presenter.prepareSuccessView(new DeleteNoteOutputData("1"));

        assertTrue(viewModel.getNotes().isEmpty());
        assertEquals(NotesViewModel.NOTE_DELETED, events.get(events.size() - 1));
    }

    @Test
    void failureStoresTheErrorMessage() {
        presenter.prepareFailView("Note not found.");

        assertEquals("Note not found.", viewModel.getErrorMessage());
        assertEquals(NotesViewModel.ERROR, events.get(events.size() - 1));
    }

    private static Note note(String id, String title) {
        return new Note(id, title, "body", List.of("tag"),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z"));
    }
}
