package interface_adapter.notes;

import entity.Note;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesViewModelTest {
    @Test
    void returnsSnapshotsThatCannotChangeTheStoredNotes() {
        NotesViewModel viewModel = new NotesViewModel();
        Note note = note("1", "Title");

        viewModel.replaceNotes(List.of(note), List.of("tag"));
        List<Note> snapshot = viewModel.getNotes();
        viewModel.noteDeleted("1");

        assertNotSame(snapshot, viewModel.getNotes());
        assertEquals(1, snapshot.size());
        assertTrue(viewModel.getNotes().isEmpty());
    }

    @Test
    void keepsInsertionOrderAndUpdatesById() {
        NotesViewModel viewModel = new NotesViewModel();
        viewModel.noteSaved(note("1", "First"));
        viewModel.noteSaved(note("2", "Second"));
        viewModel.noteSaved(note("1", "Updated"));

        assertEquals(List.of("Updated", "Second"),
                List.of(viewModel.getNotes().get(0).getTitle(),
                        viewModel.getNotes().get(1).getTitle()));
    }

    @Test
    void agentEditingFiresTransientEvents() {
        NotesViewModel viewModel = new NotesViewModel();
        List<Boolean> states = new ArrayList<>();
        viewModel.addPropertyChangeListener(event -> {
            if (NotesViewModel.AGENT_EDITING.equals(event.getPropertyName())) {
                states.add((Boolean) event.getNewValue());
            }
        });

        viewModel.setAgentEditing(true);
        viewModel.setAgentEditing(false);

        assertEquals(List.of(true, false), states);
    }

    @Test
    void storesAndClearsErrors() {
        NotesViewModel viewModel = new NotesViewModel();

        viewModel.showError("bad");
        assertEquals("bad", viewModel.getErrorMessage());

        viewModel.replaceNotes(List.of(), List.of());
        assertEquals("", viewModel.getErrorMessage());
    }

    private static Note note(String id, String title) {
        return new Note(id, title, "", List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z"));
    }
}
