package view;

import data_access.SqliteDatabase;
import data_access.SqliteNoteDAO;
import entity.Note;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentViewModel;
import interface_adapter.notes.NotesController;
import interface_adapter.notes.NotesPresenter;
import interface_adapter.notes.NotesViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.agent.AgentInputBoundary;
import use_case.agent.AgentInputData;
import use_case.notes.create_note.CreateNoteInteractor;
import use_case.notes.delete_note.DeleteNoteInteractor;
import use_case.notes.list_notes.ListNotesInteractor;
import use_case.notes.update_note.UpdateNoteInteractor;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesPanelTest {
    @TempDir
    Path tempDir;

    private SqliteNoteDAO dataAccess;
    private NotesController controller;
    private NotesPanel panel;
    private boolean confirm;
    private List<AgentInputData> agentInputs;

    @BeforeEach
    void setUp() throws Exception {
        SqliteDatabase database = new SqliteDatabase(tempDir.resolve("notes.db"));
        database.initializeSchema();
        dataAccess = new SqliteNoteDAO(database);
        NotesViewModel viewModel = new NotesViewModel();
        NotesPresenter presenter = new NotesPresenter(viewModel);
        controller = new NotesController(
                new CreateNoteInteractor(dataAccess, presenter),
                new ListNotesInteractor(dataAccess, presenter),
                new UpdateNoteInteractor(dataAccess, presenter),
                new DeleteNoteInteractor(dataAccess, presenter));
        confirm = true;
        agentInputs = new ArrayList<>();
        AgentViewModel agentViewModel = new AgentViewModel();
        AgentInputBoundary agentBoundary = input -> {
            agentInputs.add(input);
            agentViewModel.setResponse("Agent response");
        };
        FutureTask<NotesPanel> task = new FutureTask<>(() -> new NotesPanel(
                controller, viewModel, new AgentController(agentBoundary), agentViewModel,
                message -> confirm));
        SwingUtilities.invokeAndWait(task);
        panel = task.get();
        SwingUtilities.invokeAndWait(() -> panel.refresh());
    }

    @Test
    void createsANoteAndShowsItInTheList() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.titleField.setText("Algorithms");
            panel.tagsField.setText("CSC207, Study");
            panel.markdownArea.setText("# Big O\n\n**O(n log n)**");
            assertTrue(panel.saveButton.isEnabled());
            panel.saveButton.doClick(0);
        });

        assertEquals(1, panel.listModel.size());
        Note saved = panel.listModel.get(0);
        assertEquals("Algorithms", saved.getTitle());
        assertEquals("# Big O\n\n**O(n log n)**", saved.getMarkdown());
        assertEquals(java.util.List.of("CSC207", "Study"), saved.getTags());
        assertTrue(dataAccess.findById(saved.getId()).isPresent());
        assertEquals("Note saved.", panel.statusLabel.getText());
    }

    @Test
    void rendersMarkdownInThePreview() throws Exception {
        dataAccess.save(new Note("1", "Markdown", "# Heading\n\n**bold**", java.util.List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
        });

        String preview = panel.previewPane.getText();
        assertTrue(preview.contains("Heading"));
        assertTrue(preview.contains("<strong>bold</strong>"));
        assertTrue(panel.markdownArea.getText().contains("# Heading"));
    }

    @Test
    void searchAndTagFilterReloadTheList() throws Exception {
        dataAccess.save(new Note("1", "Alpha", "alpha body", java.util.List.of("CSC207"),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        dataAccess.save(new Note("2", "Beta", "beta body", java.util.List.of("Personal"),
                Instant.parse("2026-11-27T09:00:00Z"),
                Instant.parse("2026-11-27T09:00:00Z")));

        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            assertEquals(2, panel.listModel.size());
            panel.searchField.setText("alpha");
            panel.refresh();
            assertEquals(1, panel.listModel.size());
            assertEquals("Alpha", panel.listModel.get(0).getTitle());
            panel.searchField.setText("");
            panel.refresh();
            panel.tagFilter.setSelectedItem("Personal");
        });

        assertEquals(1, panel.listModel.size());
        assertEquals("Beta", panel.listModel.get(0).getTitle());
    }

    @Test
    void externalAgentWriteRefreshesTheList() throws Exception {
        Thread worker = new Thread(() -> controller.createNote("Agent note", "body", "tag"));
        worker.start();
        worker.join();
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, panel.listModel.size());
        assertEquals("Agent note", panel.listModel.get(0).getTitle());
    }

    @Test
    void externalUpdateDoesNotClobberDirtyEdits() throws Exception {
        dataAccess.save(new Note("1", "Original", "original body", java.util.List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
            panel.markdownArea.setText("my unsaved edit");
        });

        Thread worker = new Thread(() -> controller.updateNote("1", "Original", "agent body", ""));
        worker.start();
        worker.join();
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("my unsaved edit", panel.markdownArea.getText());
        assertTrue(panel.statusLabel.getText().contains("assistant changed"));
    }

    @Test
    void deleteRemovesTheSelectedNote() throws Exception {
        dataAccess.save(new Note("1", "Delete me", "", java.util.List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
            panel.deleteButton.doClick(0);
        });

        assertTrue(panel.listModel.isEmpty());
        assertTrue(dataAccess.findById("1").isEmpty());
        assertEquals("Note deleted.", panel.statusLabel.getText());
    }

    @Test
    void agentUpdateShowsDiffInTheChangesTab() throws Exception {
        dataAccess.save(new Note("1", "Title", "old body", List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
        });

        Thread worker = new Thread(() -> controller.updateNote("1", "Title", "new body", ""));
        worker.start();
        worker.join();
        SwingUtilities.invokeAndWait(() -> { });

        assertTrue(panel.previewTabs.isEnabledAt(1));
        assertEquals(1, panel.previewTabs.getSelectedIndex());
        String diff = panel.diffPane.getText();
        assertTrue(diff.contains("- old body"));
        assertTrue(diff.contains("+ new body"));
    }

    @Test
    void quoteSelectionAddsTextToTheAgentChat() throws Exception {
        dataAccess.save(new Note("1", "Calculus", "the chain rule is useful", List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
            panel.markdownArea.select(0, "the chain rule".length());
            assertTrue(panel.quoteButton.isEnabled());
            panel.quoteButton.doClick(0);
        });

        String transcript = panel.notesAgentPanel.transcript.getText();
        assertTrue(transcript.contains("Quoted"));
        assertTrue(transcript.contains("the chain rule"));
        assertTrue(panel.notesAgentPanel.input.isEnabled());
    }

    @Test
    void cancellingDiscardKeepsTheDirtyEditor() throws Exception {
        dataAccess.save(new Note("1", "First", "first body", java.util.List.of(),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z")));
        dataAccess.save(new Note("2", "Second", "second body", java.util.List.of(),
                Instant.parse("2026-11-27T09:00:00Z"),
                Instant.parse("2026-11-27T09:00:00Z")));
        confirm = false;
        SwingUtilities.invokeAndWait(() -> {
            panel.refresh();
            panel.noteList.setSelectedIndex(0);
            panel.markdownArea.setText("my unsaved edit");
            panel.noteList.setSelectedIndex(1);
        });

        assertEquals("First", panel.titleField.getText());
        assertEquals("my unsaved edit", panel.markdownArea.getText());
        assertEquals(0, panel.noteList.getSelectedIndex());
        assertTrue(panel.saveButton.isEnabled());
    }
}
