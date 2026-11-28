package view;

import entity.Note;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentViewModel;
import org.junit.jupiter.api.Test;
import use_case.agent.AgentInputBoundary;
import use_case.agent.AgentInputData;

import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesAgentPanelTest {
    @Test
    void quickPromptsIncludeTheNoteIdAndTitle() {
        Note note = note();

        assertTrue(NotesAgentPanel.summarizePrompt(note).contains("n1"));
        assertTrue(NotesAgentPanel.summarizePrompt(note).contains("Calculus"));
        assertTrue(NotesAgentPanel.rewritePrompt(note).contains("update_note"));
        assertTrue(NotesAgentPanel.tagsPrompt(note).contains("3-5"));
        assertTrue(NotesAgentPanel.continuePrompt(note).contains("append"));
        assertTrue(NotesAgentPanel.explainPrompt(note).contains("plain language"));
        assertTrue(NotesAgentPanel.freeFormPrompt(note, "What is this?")
                .contains("What is this?"));
    }

    @Test
    void controlsRequireASavedNoteAndNoDirtyEdits() throws Exception {
        AgentViewModel viewModel = new AgentViewModel();
        NotesAgentPanel panel = createPanel(viewModel, input -> { });

        SwingUtilities.invokeAndWait(() -> {
            panel.setContext(null, false);
            assertFalse(panel.summarizeButton.isEnabled());
            assertFalse(panel.input.isEnabled());

            panel.setContext(note(), true);
            assertFalse(panel.summarizeButton.isEnabled());
            assertFalse(panel.input.isEnabled());

            panel.setContext(note(), false);
            assertTrue(panel.summarizeButton.isEnabled());
            assertTrue(panel.rewriteButton.isEnabled());
            assertTrue(panel.tagsButton.isEnabled());
            assertTrue(panel.continueButton.isEnabled());
            assertTrue(panel.explainButton.isEnabled());
            assertTrue(panel.input.isEnabled());
            assertTrue(panel.sendButton.isEnabled());
        });
    }

    @Test
    void freeFormSendPassesTheNoteContextToTheAgent() throws Exception {
        AgentViewModel viewModel = new AgentViewModel();
        List<AgentInputData> sent = new ArrayList<>();
        AgentInputBoundary boundary = input -> {
            sent.add(input);
            viewModel.setResponse("Agent answer");
            viewModel.setErrorMessage("");
        };
        NotesAgentPanel panel = createPanel(viewModel, boundary);
        SwingUtilities.invokeAndWait(() -> {
            panel.setContext(note(), false);
            panel.input.setText("What is this?");
            panel.sendButton.doClick(0);
        });
        waitFor(panel, "Agent answer");

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).getUserMessage().contains("n1"));
        assertTrue(sent.get(0).getUserMessage().contains("What is this?"));
        assertEquals("gpt-6.1-sol", sent.get(0).getModel());
        assertEquals("medium", sent.get(0).getEffort());
        assertTrue(panel.transcript.getText().contains("Agent: Agent answer"));
    }

    @Test
    void quotedSelectionIsIncludedInTheNextPrompt() throws Exception {
        AgentViewModel viewModel = new AgentViewModel();
        List<AgentInputData> sent = new ArrayList<>();
        AgentInputBoundary boundary = input -> {
            sent.add(input);
            viewModel.setResponse("Agent answer");
            viewModel.setErrorMessage("");
        };
        NotesAgentPanel panel = createPanel(viewModel, boundary);
        SwingUtilities.invokeAndWait(() -> {
            panel.setContext(note(), false);
            panel.addQuote("Calculus", "the chain rule");
            panel.input.setText("Explain this");
            panel.sendButton.doClick(0);
        });
        waitFor(panel, "Agent answer");
        waitFor(panel, "Quoted: From \"Calculus\"");

        assertEquals(1, sent.size());
        String prompt = sent.get(0).getUserMessage();
        assertTrue(prompt.contains("Quoted selections from the current note"));
        assertTrue(prompt.contains("the chain rule"));
        assertTrue(prompt.contains("Explain this"));
    }

    @Test
    void activityListenerReceivesBusyChanges() throws Exception {
        AgentViewModel viewModel = new AgentViewModel();
        List<Boolean> busyStates = new ArrayList<>();
        AgentInputBoundary boundary = input -> {
            viewModel.setResponse("Done.");
            viewModel.setErrorMessage("");
        };
        NotesAgentPanel panel = createPanel(viewModel, boundary);
        panel.setActivityListener(busyStates::add);
        SwingUtilities.invokeAndWait(() -> {
            panel.setContext(note(), false);
            panel.summarizeButton.doClick(0);
        });
        waitFor(panel, "Done.");

        assertTrue(busyStates.contains(true));
        assertTrue(busyStates.contains(false));
    }

    @Test
    void quickActionSendsItsPromptAndShowsTheResponse() throws Exception {
        AgentViewModel viewModel = new AgentViewModel();
        List<AgentInputData> sent = new ArrayList<>();
        AgentInputBoundary boundary = input -> {
            sent.add(input);
            viewModel.setResponse("The summary.");
            viewModel.setErrorMessage("");
        };
        NotesAgentPanel panel = createPanel(viewModel, boundary);
        SwingUtilities.invokeAndWait(() -> {
            panel.setContext(note(), false);
            panel.summarizeButton.doClick(0);
        });
        waitFor(panel, "The summary.");

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).getUserMessage().contains("Summarize note n1"));
        assertTrue(panel.transcript.getText().contains("You: Summarize this note."));
        assertTrue(panel.transcript.getText().contains("Agent: The summary."));
    }

    private static NotesAgentPanel createPanel(AgentViewModel viewModel,
                                               AgentInputBoundary boundary) throws Exception {
        FutureTask<NotesAgentPanel> task = new FutureTask<>(
                () -> new NotesAgentPanel(new AgentController(boundary), viewModel));
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void waitFor(NotesAgentPanel panel, String text) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> { });
            if (panel.transcript.getText().contains(text)) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for: " + text);
    }

    private static Note note() {
        return new Note("n1", "Calculus", "derivatives", List.of("Math"),
                Instant.parse("2026-11-27T10:00:00Z"),
                Instant.parse("2026-11-27T10:00:00Z"));
    }
}
