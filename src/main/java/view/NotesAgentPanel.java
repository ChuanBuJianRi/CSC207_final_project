package view;

import entity.Note;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A compact agent assistant scoped to the note currently open in the Notes tab.
 */
public final class NotesAgentPanel extends JPanel {
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ERROR = new Color(185, 28, 28);
    private static final int MAX_QUOTE_CHARS = 8_000;

    private final AgentController agentController;
    private final AgentViewModel agentViewModel;

    final JComboBox<String> modelBox = new JComboBox<>(AgentModels.MODELS);
    final JComboBox<String> effortBox = new JComboBox<>(AgentModels.EFFORT_LABELS);
    final JLabel contextLabel = new JLabel("No note selected.");
    final JLabel statusLabel = new JLabel(" ");
    final JTextArea transcript = new JTextArea(12, 26);
    final JTextField input = new JTextField();
    final JButton sendButton = new JButton("Send");
    final JButton summarizeButton = new JButton("Summarize");
    final JButton rewriteButton = new JButton("Rewrite");
    final JButton tagsButton = new JButton("Generate tags");
    final JButton continueButton = new JButton("Continue");
    final JButton explainButton = new JButton("Explain");

    private final List<String> pendingQuotes = new ArrayList<>();

    private ActivityListener activityListener;
    private Note currentNote;
    private boolean dirty;
    private boolean busy;

    @FunctionalInterface
    public interface ActivityListener {
        void agentActivityChanged(boolean busy);
    }

    public NotesAgentPanel(AgentController agentController, AgentViewModel agentViewModel) {
        this.agentController = agentController;
        this.agentViewModel = agentViewModel;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Agent"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        setPreferredSize(new Dimension(320, 480));
        setMinimumSize(new Dimension(260, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTranscript(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        summarizeButton.addActionListener(event -> sendQuick(
                "Summarize this note.", summarizePrompt(currentNote)));
        rewriteButton.addActionListener(event -> sendQuick(
                "Rewrite this note.", rewritePrompt(currentNote)));
        tagsButton.addActionListener(event -> sendQuick(
                "Generate tags for this note.", tagsPrompt(currentNote)));
        continueButton.addActionListener(event -> sendQuick(
                "Continue this note.", continuePrompt(currentNote)));
        explainButton.addActionListener(event -> sendQuick(
                "Explain this note.", explainPrompt(currentNote)));
        sendButton.addActionListener(event -> sendFreeForm());
        input.addActionListener(event -> sendFreeForm());

        modelBox.setEditable(true);
        effortBox.setSelectedIndex(1);
        updateControls();
    }

    public void setActivityListener(ActivityListener listener) {
        this.activityListener = listener;
    }

    private void notifyActivity(boolean busy) {
        if (activityListener != null) {
            activityListener.agentActivityChanged(busy);
        }
    }

    /**
     * Updates the note and dirty state the agent should work with.
     */
    public void setContext(Note note, boolean dirty) {
        String previousId = currentNote == null ? null : currentNote.getId();
        String newId = note == null ? null : note.getId();
        if (!Objects.equals(previousId, newId)) {
            transcript.setText("");
            pendingQuotes.clear();
        }
        this.currentNote = note;
        this.dirty = dirty;
        updateControls();
    }

    /**
     * Adds a quoted selection to the pending context for the next agent message.
     */
    void addQuote(String noteTitle, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String quote = text.strip();
        if (quote.length() > MAX_QUOTE_CHARS) {
            quote = quote.substring(0, MAX_QUOTE_CHARS) + "\n[quote truncated]";
        }
        pendingQuotes.add(quote);
        String title = noteTitle == null || noteTitle.isBlank() ? "note" : noteTitle;
        append("Quoted", "From \"" + title + "\":\n" + quote);
        setStatus("Quote attached. Ask the agent about it.", false);
        if (input.isEnabled()) {
            input.requestFocusInWindow();
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        contextLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        contextLabel.setForeground(MUTED);
        header.add(contextLabel, BorderLayout.NORTH);

        JPanel controls = new JPanel(new GridLayout(2, 2, 6, 4));
        controls.setOpaque(false);
        controls.add(new JLabel("Model"));
        controls.add(modelBox);
        controls.add(new JLabel("Effort"));
        controls.add(effortBox);
        header.add(controls, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6));
        actions.setOpaque(false);
        actions.add(summarizeButton);
        actions.add(rewriteButton);
        actions.add(tagsButton);
        actions.add(continueButton);
        actions.add(explainButton);
        header.add(actions, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTranscript() {
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        transcript.getAccessibleContext().setAccessibleName("Agent conversation");
        return new JScrollPane(transcript);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        footer.setOpaque(false);
        input.getAccessibleContext().setAccessibleName("Message to agent");
        footer.add(input, BorderLayout.CENTER);
        footer.add(sendButton, BorderLayout.EAST);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.SOUTH);
        return footer;
    }

    private void sendQuick(String display, String prompt) {
        if (!canUseAgent() || prompt == null) {
            return;
        }
        append("You", display);
        execute(prompt);
    }

    private void sendFreeForm() {
        if (!canUseAgent()) {
            return;
        }
        String text = input.getText().trim();
        if (text.isEmpty()) {
            setStatus("Enter a message.", true);
            return;
        }
        input.setText("");
        append("You", text);
        execute(freeFormPrompt(currentNote, text));
    }

    private void execute(String prompt) {
        if (!canUseAgent()) {
            return;
        }
        String model = String.valueOf(modelBox.getEditor().getItem()).trim();
        int effortIndex = Math.max(0,
                Math.min(AgentModels.EFFORTS.length - 1, effortBox.getSelectedIndex()));
        String effort = AgentModels.EFFORTS[effortIndex];
        String fullPrompt = withQuotes(prompt);
        pendingQuotes.clear();
        busy = true;
        updateControls();
        setStatus("Thinking...", false);
        notifyActivity(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                agentController.execute(fullPrompt, model, effort);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    append("Agent", "The request was interrupted.");
                } catch (RuntimeException exception) {
                    append("Agent", "The request failed: " + exception.getMessage());
                } catch (Exception exception) {
                    append("Agent", "The request failed: " + exception.getMessage());
                }
                showResult();
            }
        }.execute();
    }

    private void showResult() {
        String error = agentViewModel.getErrorMessage();
        if (error != null && !error.isBlank()) {
            append("Agent", error);
            busy = false;
            updateControls();
            notifyActivity(false);
            setStatus("Agent request failed.", true);
            return;
        }
        String response = agentViewModel.getResponse();
        append("Agent", response == null || response.isBlank() ? "(no response)" : response);
        busy = false;
        updateControls();
        notifyActivity(false);
        setStatus("Ready.", false);
    }

    private String withQuotes(String prompt) {
        if (pendingQuotes.isEmpty()) {
            return prompt;
        }
        StringBuilder builder = new StringBuilder("Quoted selections from the current note:\n");
        for (String quote : pendingQuotes) {
            builder.append("\"\"\"\n").append(quote).append("\n\"\"\"\n\n");
        }
        builder.append(prompt);
        return builder.toString();
    }

    private void append(String speaker, String text) {
        transcript.append(speaker + ": " + text + "\n\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    private boolean canUseAgent() {
        return currentNote != null
                && currentNote.getId() != null
                && !currentNote.getId().isBlank()
                && !dirty
                && !busy;
    }

    private void updateControls() {
        boolean enabled = canUseAgent();
        for (JButton button : new JButton[]{
                summarizeButton, rewriteButton, tagsButton, continueButton,
                explainButton, sendButton}) {
            button.setEnabled(enabled);
        }
        input.setEnabled(enabled);
        modelBox.setEnabled(!busy);
        effortBox.setEnabled(!busy);

        if (currentNote == null || currentNote.getId() == null
                || currentNote.getId().isBlank()) {
            contextLabel.setText("No note selected.");
            setStatus("Select a note to use the agent.", false);
        } else if (dirty) {
            contextLabel.setText("Save changes to use the agent.");
            setStatus("Save your changes first.", false);
        } else if (busy) {
            contextLabel.setText("Working on: " + currentNote.getTitle());
            setStatus("Thinking...", false);
        } else {
            contextLabel.setText("Working on: " + currentNote.getTitle());
            setStatus("Ready.", false);
        }
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? ERROR : MUTED);
    }

    static String summarizePrompt(Note note) {
        return note == null ? null : "Summarize note " + note.getId()
                + " (title: \"" + note.getTitle() + "\"). Use read_note first. "
                + "Return a concise summary for the user.";
    }

    static String rewritePrompt(Note note) {
        return note == null ? null : "Rewrite note " + note.getId()
                + " (title: \"" + note.getTitle() + "\") to improve clarity and structure. "
                + "Use read_note, then update_note with the improved Markdown. Keep the title "
                + "and tags unless a small title improvement is clearly useful.";
    }

    static String tagsPrompt(Note note) {
        return note == null ? null : "Read note " + note.getId()
                + " and update its tags with 3-5 relevant tags using update_note. "
                + "Do not change its title or markdown.";
    }

    static String continuePrompt(Note note) {
        return note == null ? null : "Read note " + note.getId()
                + " and append a short, useful continuation to its Markdown using update_note. "
                + "Keep the existing content unchanged.";
    }

    static String explainPrompt(Note note) {
        return note == null ? null : "Read note " + note.getId()
                + " and explain its main ideas in plain language for a student.";
    }

    static String freeFormPrompt(Note note, String text) {
        if (note == null) {
            return text;
        }
        return "You are helping with note " + note.getId() + " (title: \""
                + note.getTitle() + "\"). " + text + " Use the notes tools when needed.";
    }
}
