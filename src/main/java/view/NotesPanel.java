package view;

import entity.Note;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentViewModel;
import interface_adapter.notes.NotesController;
import interface_adapter.notes.NotesViewModel;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class NotesPanel extends JPanel {
    public static final String ALL_TAGS = "All tags";

    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ERROR = new Color(185, 28, 28);

    private static final DateTimeFormatter UPDATED_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private final NotesController controller;
    private final NotesViewModel viewModel;
    final NotesAgentPanel notesAgentPanel;
    private final ConfirmAction confirmAction;

    final JTextField searchField = new JTextField(18);
    final JComboBox<String> tagFilter = new JComboBox<>();
    final JButton newButton = new JButton("New");
    final JButton deleteButton = new JButton("Delete");
    final JButton quoteButton = new JButton("Quote to agent");
    final DefaultListModel<Note> listModel = new DefaultListModel<>();
    final JList<Note> noteList = new JList<>(listModel);
    final JTextField titleField = new JTextField();
    final JTextField tagsField = new JTextField();
    final JButton saveButton = new JButton("Save");
    final JTextArea markdownArea = new JTextArea(18, 40);
    final JEditorPane previewPane = new JEditorPane();
    final JTabbedPane previewTabs = new JTabbedPane();
    final JTextPane diffPane = new JTextPane();
    final JLabel diffSummary = new JLabel(" ");
    final JPanel agentBanner = new JPanel(new BorderLayout(8, 0));
    final JLabel agentBannerLabel = new JLabel("Agent is working on this note...");
    final JProgressBar agentProgress = new JProgressBar();
    final JLabel statusLabel = new JLabel(" ");

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();
    private final Timer previewTimer;
    private final Timer searchTimer;
    private final Timer agentPulseTimer;
    private final Timer diffFlashTimer;
    private long diffFlashStart;
    private boolean notesAgentBusy;
    private boolean noteToolEditing;

    private Note currentNote;
    private JTextComponent lastSelectionSource;
    private String currentNoteId;
    private String lastSavedId;
    private boolean dirty;
    private boolean ownSaveInProgress;
    private boolean suppressDocumentEvents;
    private boolean suppressSelection;
    private boolean suppressTagEvents;
    private boolean reloading;

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String message);
    }

    public NotesPanel(NotesController controller, NotesViewModel viewModel) {
        this(controller, viewModel, (ConfirmAction) null);
    }

    public NotesPanel(NotesController controller, NotesViewModel viewModel,
                      ConfirmAction confirmAction) {
        this(controller, viewModel, null, null, confirmAction);
    }

    public NotesPanel(NotesController controller, NotesViewModel viewModel,
                      AgentController agentController, AgentViewModel agentViewModel) {
        this(controller, viewModel, agentController, agentViewModel, null);
    }

    public NotesPanel(NotesController controller, NotesViewModel viewModel,
                      AgentController agentController, AgentViewModel agentViewModel,
                      ConfirmAction confirmAction) {
        this.controller = controller;
        this.viewModel = viewModel;
        this.confirmAction = confirmAction == null ? defaultConfirmAction() : confirmAction;
        this.notesAgentPanel = agentController == null
                ? null : new NotesAgentPanel(agentController, agentViewModel);
        this.previewTimer = new Timer(300, event -> updatePreview());
        this.previewTimer.setRepeats(false);
        this.searchTimer = new Timer(300, event -> reloadList());
        this.searchTimer.setRepeats(false);
        this.agentPulseTimer = new Timer(50, event -> pulseAgentBanner());
        this.diffFlashTimer = new Timer(30, event -> animateDiffSummary());

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        installListeners();
        viewModel.addPropertyChangeListener(event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                handleViewEvent(event);
            } else {
                SwingUtilities.invokeLater(() -> handleViewEvent(event));
            }
        });
        if (notesAgentPanel != null) {
            notesAgentPanel.setActivityListener(this::setAgentBusy);
        }
        setEditorEnabled(false);
        updatePreview();
        updateAgentContext();
    }

    private static ConfirmAction defaultConfirmAction() {
        return message -> JOptionPane.showConfirmDialog(
                null, message, "Confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Reloads the list using the current search text and tag filter.
     */
    public void refresh() {
        reloadList();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        searchField.setToolTipText("Search note titles and markdown");
        searchField.getAccessibleContext().setAccessibleName("Search notes");
        tagFilter.getAccessibleContext().setAccessibleName("Filter by tag");
        tagFilter.addItem(ALL_TAGS);
        toolbar.add(new JLabel("Search"));
        toolbar.add(searchField);
        toolbar.add(new JLabel("Tag"));
        toolbar.add(tagFilter);
        toolbar.add(newButton);
        toolbar.add(deleteButton);
        if (notesAgentPanel != null) {
            quoteButton.setToolTipText(
                    "Quote the selected Markdown or preview text into the agent chat");
            toolbar.add(quoteButton);
        }
        return toolbar;
    }

    private JSplitPane buildCenter() {
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.getAccessibleContext().setAccessibleName("Notes");
        noteList.setCellRenderer(new NoteRenderer());
        noteList.setVisibleRowCount(18);
        JScrollPane listScroll = new JScrollPane(noteList);
        listScroll.setPreferredSize(new Dimension(260, 420));

        markdownArea.setLineWrap(true);
        markdownArea.setWrapStyleWord(true);
        markdownArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        markdownArea.getAccessibleContext().setAccessibleName("Markdown");

        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        previewPane.setBackground(Color.WHITE);
        previewPane.getAccessibleContext().setAccessibleName("Markdown preview");

        diffPane.setEditable(false);
        diffPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        diffPane.setBackground(Color.WHITE);
        diffPane.getAccessibleContext().setAccessibleName("Agent changes");
        diffSummary.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        diffSummary.setForeground(MUTED);
        JPanel diffPanel = new JPanel(new BorderLayout(0, 4));
        diffPanel.setOpaque(false);
        diffPanel.add(diffSummary, BorderLayout.NORTH);
        diffPanel.add(new JScrollPane(diffPane), BorderLayout.CENTER);

        previewTabs.addTab("Preview", new JScrollPane(previewPane));
        previewTabs.addTab("Changes", diffPanel);
        previewTabs.setEnabledAt(1, false);
        previewTabs.getAccessibleContext().setAccessibleName("Preview or agent changes");

        JSplitPane sourcePreview = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                titledScroll("Markdown", markdownArea),
                previewTabs);
        sourcePreview.setResizeWeight(0.5);
        sourcePreview.setDividerLocation(0.5);
        sourcePreview.setBorder(null);

        JPanel editorContent = new JPanel(new BorderLayout(0, 8));
        editorContent.setOpaque(false);
        editorContent.add(buildEditorForm(), BorderLayout.NORTH);
        editorContent.add(sourcePreview, BorderLayout.CENTER);

        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.setOpaque(false);
        editor.add(buildAgentBanner(), BorderLayout.NORTH);
        editor.add(editorContent, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, editor);
        split.setResizeWeight(0.0);
        split.setDividerLocation(260);
        split.setBorder(null);
        if (notesAgentPanel == null) {
            return split;
        }
        JSplitPane withAgent = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, split, notesAgentPanel);
        withAgent.setResizeWeight(0.72);
        withAgent.setDividerLocation(0.72);
        withAgent.setBorder(null);
        return withAgent;
    }

    private JPanel buildEditorForm() {
        JPanel form = new JPanel(new BorderLayout(8, 0));
        form.setOpaque(false);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 4));
        rows.setOpaque(false);
        rows.add(labelledField("Title", titleField));
        rows.add(labelledField("Tags (comma separated)", tagsField));
        form.add(rows, BorderLayout.CENTER);
        form.add(saveButton, BorderLayout.EAST);
        return form;
    }

    private static JPanel labelledField(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel title = new JLabel(label);
        title.setForeground(MUTED);
        row.add(title, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        field.getAccessibleContext().setAccessibleName(label);
        return row;
    }

    private static JScrollPane titledScroll(String title, JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.CENTER);
        return footer;
    }

    private JPanel buildAgentBanner() {
        agentBanner.setOpaque(true);
        agentBanner.setBackground(new Color(219, 234, 254));
        agentBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 197, 253)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        agentBannerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        agentBannerLabel.setForeground(new Color(30, 64, 175));
        agentProgress.setIndeterminate(true);
        agentProgress.setBorderPainted(false);
        agentProgress.setPreferredSize(new Dimension(110, 8));
        agentBanner.add(agentBannerLabel, BorderLayout.CENTER);
        agentBanner.add(agentProgress, BorderLayout.EAST);
        agentBanner.setVisible(false);
        return agentBanner;
    }

    private void setAgentBusy(boolean busy) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setAgentBusy(busy));
            return;
        }
        notesAgentBusy = busy;
        updateAgentBanner();
    }

    private void setNoteToolEditing(boolean editing) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setNoteToolEditing(editing));
            return;
        }
        noteToolEditing = editing;
        updateAgentBanner();
    }

    private void updateAgentBanner() {
        boolean busy = notesAgentBusy || noteToolEditing;
        agentBanner.setVisible(busy);
        agentProgress.setIndeterminate(busy);
        if (busy) {
            agentPulseTimer.start();
            setStatus("Agent is working on this note...", false);
        } else {
            agentPulseTimer.stop();
            agentBanner.setBackground(new Color(219, 234, 254));
            if (previewTabs.isEnabledAt(1)) {
                setStatus("Agent finished. Changes are available in the Changes tab.", false);
            } else {
                setStatus("Agent finished.", false);
            }
        }
        revalidate();
        repaint();
    }

    private void pulseAgentBanner() {
        if (!agentBanner.isVisible()) {
            return;
        }
        double phase = (System.currentTimeMillis() % 1200) / 1200.0;
        double amount = 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);
        agentBanner.setBackground(blend(
                new Color(219, 234, 254), new Color(191, 219, 254), amount));
    }

    private boolean showDiff(Note oldNote, Note newNote) {
        NoteDiff.Result result = NoteDiff.compute(oldNote.getMarkdown(), newNote.getMarkdown());
        boolean titleChanged = !Objects.equals(oldNote.getTitle(), newNote.getTitle());
        boolean tagsChanged = !oldNote.getTags().equals(newNote.getTags());
        if (result.isEmpty() && !titleChanged && !tagsChanged) {
            clearDiff();
            return false;
        }
        NoteDiff.render(diffPane, result);
        StringBuilder summary = new StringBuilder("Agent update: +")
                .append(result.getAdded()).append(" −").append(result.getDeleted())
                .append(" lines");
        if (titleChanged) {
            summary.append(" · title: \"").append(oldNote.getTitle())
                    .append("\" → \"").append(newNote.getTitle()).append("\"");
        }
        if (tagsChanged) {
            summary.append(" · tags: ").append(oldNote.getTags())
                    .append(" → ").append(newNote.getTags());
        }
        diffSummary.setText(summary.toString());
        previewTabs.setTitleAt(1, "Changes (+" + result.getAdded()
                + " −" + result.getDeleted() + ")");
        previewTabs.setEnabledAt(1, true);
        previewTabs.setSelectedIndex(1);
        flashDiffSummary();
        return true;
    }

    private void clearDiff() {
        diffFlashTimer.stop();
        diffSummary.setOpaque(false);
        diffPane.setText("");
        diffSummary.setText(" ");
        previewTabs.setTitleAt(1, "Changes");
        previewTabs.setEnabledAt(1, false);
        if (previewTabs.getSelectedIndex() == 1) {
            previewTabs.setSelectedIndex(0);
        }
    }

    private void flashDiffSummary() {
        diffFlashStart = System.currentTimeMillis();
        diffSummary.setOpaque(true);
        diffFlashTimer.start();
    }

    private void animateDiffSummary() {
        double progress = Math.min(1.0,
                (System.currentTimeMillis() - diffFlashStart) / 700.0);
        diffSummary.setOpaque(true);
        diffSummary.setBackground(blend(
                new Color(254, 249, 195), Color.WHITE, progress));
        diffSummary.repaint();
        if (progress >= 1.0) {
            diffFlashTimer.stop();
            diffSummary.setOpaque(false);
        }
    }

    private static Color blend(Color from, Color to, double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(red, green, blue);
    }

    private void installListeners() {
        newButton.addActionListener(event -> startDraft());
        saveButton.addActionListener(event -> save());
        deleteButton.addActionListener(event -> deleteCurrent());
        tagFilter.addActionListener(event -> {
            if (!suppressTagEvents) {
                reloadList();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchTimer.restart();
            }
        });
        DocumentListener dirtyListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                markDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                markDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                markDirty();
            }
        };
        titleField.getDocument().addDocumentListener(dirtyListener);
        tagsField.getDocument().addDocumentListener(dirtyListener);
        markdownArea.getDocument().addDocumentListener(dirtyListener);
        noteList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        quoteButton.addActionListener(event -> quoteSelection());
        markdownArea.addCaretListener(event -> updateQuoteButton());
        previewPane.addCaretListener(event -> updateQuoteButton());
        FocusAdapter selectionFocus = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (event.getSource() instanceof JTextComponent) {
                    lastSelectionSource = (JTextComponent) event.getSource();
                }
                updateQuoteButton();
            }
        };
        MouseAdapter selectionMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getSource() instanceof JTextComponent) {
                    lastSelectionSource = (JTextComponent) event.getSource();
                }
                updateQuoteButton();
            }
        };
        markdownArea.addFocusListener(selectionFocus);
        previewPane.addFocusListener(selectionFocus);
        markdownArea.addMouseListener(selectionMouse);
        previewPane.addMouseListener(selectionMouse);
    }

    private void onSelectionChanged() {
        if (suppressSelection) {
            return;
        }
        Note selected = noteList.getSelectedValue();
        if (selected == null || selected.getId().equals(currentNoteId)) {
            return;
        }
        if (dirty && !confirmAction.confirm("Discard unsaved changes?")) {
            restoreSelection();
            return;
        }
        clearDiff();
        loadNote(selected);
    }

    private void restoreSelection() {
        suppressSelection = true;
        try {
            if (currentNoteId == null) {
                noteList.clearSelection();
            } else {
                for (int index = 0; index < listModel.size(); index++) {
                    if (listModel.get(index).getId().equals(currentNoteId)) {
                        noteList.setSelectedIndex(index);
                        break;
                    }
                }
            }
        } finally {
            suppressSelection = false;
        }
    }

    private void loadNote(Note note) {
        suppressDocumentEvents = true;
        try {
            titleField.setText(note.getTitle());
            tagsField.setText(String.join(", ", note.getTags()));
            markdownArea.setText(note.getMarkdown());
        } finally {
            suppressDocumentEvents = false;
        }
        currentNote = note;
        currentNoteId = note.getId();
        lastSavedId = null;
        dirty = false;
        saveButton.setEnabled(false);
        deleteButton.setEnabled(true);
        setEditorEnabled(true);
        previewTimer.stop();
        updatePreview();
        setStatus("Loaded \"" + note.getTitle() + "\".", false);
        updateAgentContext();
    }

    private void startDraft() {
        if (dirty && !confirmAction.confirm("Discard unsaved changes?")) {
            return;
        }
        clearDiff();
        suppressSelection = true;
        try {
            noteList.clearSelection();
        } finally {
            suppressSelection = false;
        }
        suppressDocumentEvents = true;
        try {
            titleField.setText("");
            tagsField.setText("");
            markdownArea.setText("");
        } finally {
            suppressDocumentEvents = false;
        }
        currentNote = null;
        currentNoteId = null;
        lastSavedId = null;
        dirty = false;
        saveButton.setEnabled(false);
        deleteButton.setEnabled(false);
        setEditorEnabled(true);
        previewTimer.stop();
        updatePreview();
        setStatus("New note. Enter a title and some markdown.", false);
        updateAgentContext();
        titleField.requestFocusInWindow();
    }

    private void save() {
        ownSaveInProgress = true;
        try {
            if (currentNoteId == null) {
                controller.createNote(titleField.getText(), markdownArea.getText(),
                        tagsField.getText());
            } else {
                controller.updateNote(currentNoteId, titleField.getText(),
                        markdownArea.getText(), tagsField.getText());
            }
        } catch (RuntimeException exception) {
            ownSaveInProgress = false;
            setStatus("Could not save the note. Try again.", true);
        }
    }

    private void deleteCurrent() {
        if (currentNoteId == null) {
            setStatus("Select a note to delete.", true);
            return;
        }
        if (!confirmAction.confirm("Delete this note permanently?")) {
            return;
        }
        try {
            controller.deleteNote(currentNoteId);
        } catch (RuntimeException exception) {
            setStatus("Could not delete the note. Try again.", true);
        }
    }

    private void markDirty() {
        if (suppressDocumentEvents) {
            return;
        }
        dirty = true;
        saveButton.setEnabled(true);
        setStatus("Unsaved changes.", false);
        previewTimer.restart();
        updateAgentContext();
    }

    private void setEditorEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        tagsField.setEnabled(enabled);
        markdownArea.setEnabled(enabled);
        saveButton.setEnabled(enabled && dirty);
        deleteButton.setEnabled(enabled && currentNoteId != null);
    }

    private void clearEditor() {
        clearDiff();
        suppressDocumentEvents = true;
        try {
            titleField.setText("");
            tagsField.setText("");
            markdownArea.setText("");
        } finally {
            suppressDocumentEvents = false;
        }
        currentNote = null;
        currentNoteId = null;
        dirty = false;
        saveButton.setEnabled(false);
        deleteButton.setEnabled(false);
        setEditorEnabled(false);
        previewTimer.stop();
        updatePreview();
        updateAgentContext();
    }

    private void reloadList() {
        if (reloading) {
            return;
        }
        reloading = true;
        try {
            controller.loadNotes(searchField.getText(), selectedTag());
        } catch (RuntimeException exception) {
            setStatus("Could not load notes. Try again.", true);
        } finally {
            reloading = false;
        }
    }

    private String selectedTag() {
        Object selected = tagFilter.getSelectedItem();
        if (selected == null || ALL_TAGS.equals(selected)) {
            return "";
        }
        return selected.toString();
    }

    private void handleViewEvent(PropertyChangeEvent event) {
        switch (event.getPropertyName()) {
            case NotesViewModel.NOTES_LOADED:
                rebuildList();
                rebuildTagFilter();
                handleCurrentSelectionMissing();
                break;
            case NotesViewModel.NOTE_SAVED:
                handleNoteSaved((Note) event.getNewValue());
                break;
            case NotesViewModel.NOTE_DELETED:
                handleNoteDeleted((String) event.getNewValue());
                break;
            case NotesViewModel.AGENT_EDITING:
                setNoteToolEditing(Boolean.TRUE.equals(event.getNewValue()));
                break;
            case NotesViewModel.ERROR:
                ownSaveInProgress = false;
                setStatus((String) event.getNewValue(), true);
                break;
            default:
                break;
        }
    }

    private void handleNoteSaved(Note note) {
        boolean wasOwnSave = ownSaveInProgress;
        ownSaveInProgress = false;
        Note previous = currentNote;
        boolean externalCurrent = !wasOwnSave && note.getId().equals(currentNoteId);
        boolean hasDiff = externalCurrent && previous != null
                && note.getId().equals(previous.getId())
                && showDiff(previous, note);
        if (wasOwnSave) {
            clearDiff();
            if (currentNoteId == null) {
                currentNoteId = note.getId();
            }
            currentNote = note;
            lastSavedId = note.getId();
            dirty = false;
            saveButton.setEnabled(false);
            deleteButton.setEnabled(true);
            setStatus("Note saved.", false);
        } else if (externalCurrent) {
            if (dirty) {
                setStatus("The assistant changed this note; your edits are unsaved.", true);
            } else {
                loadNote(note);
                setStatus(hasDiff
                        ? "Agent updated this note. Review the Changes tab."
                        : "Agent saved this note.", false);
            }
        }
        reloadList();
        updateAgentContext();
    }

    private void handleNoteDeleted(String noteId) {
        boolean wasCurrent = noteId.equals(currentNoteId);
        boolean wasDirty = dirty;
        if (wasCurrent) {
            clearDiff();
        }
        if (wasCurrent && !wasDirty) {
            clearEditor();
        } else if (wasCurrent) {
            currentNote = null;
            currentNoteId = null;
            deleteButton.setEnabled(false);
        }
        reloadList();
        if (wasCurrent) {
            if (wasDirty) {
                setStatus("This note was deleted; your edits are unsaved.", true);
            } else {
                setStatus("Note deleted.", false);
            }
        }
        updateAgentContext();
    }

    private void rebuildList() {
        String selectedId = currentNoteId;
        suppressSelection = true;
        try {
            listModel.clear();
            for (Note note : viewModel.getNotes()) {
                listModel.addElement(note);
            }
            if (selectedId != null) {
                for (int index = 0; index < listModel.size(); index++) {
                    if (listModel.get(index).getId().equals(selectedId)) {
                        noteList.setSelectedIndex(index);
                        break;
                    }
                }
            }
        } finally {
            suppressSelection = false;
        }
    }

    private void rebuildTagFilter() {
        String selected = selectedTag();
        suppressTagEvents = true;
        try {
            tagFilter.removeAllItems();
            tagFilter.addItem(ALL_TAGS);
            List<String> tags = viewModel.getTags();
            for (String tag : tags) {
                tagFilter.addItem(tag);
            }
            boolean restored = false;
            for (int index = 0; index < tagFilter.getItemCount(); index++) {
                if (tagFilter.getItemAt(index).equals(selected)) {
                    tagFilter.setSelectedIndex(index);
                    restored = true;
                    break;
                }
            }
            if (!restored) {
                tagFilter.setSelectedIndex(0);
            }
        } finally {
            suppressTagEvents = false;
        }
    }

    private void handleCurrentSelectionMissing() {
        if (currentNoteId == null) {
            if (!dirty) {
                clearEditor();
                setStatus("Select a note or create a new one.", false);
            }
            return;
        }
        for (Note note : viewModel.getNotes()) {
            if (note.getId().equals(currentNoteId)) {
                if (!dirty) {
                    currentNote = note;
                }
                updateAgentContext();
                return;
            }
        }
        if (dirty) {
            setStatus("The selected note is not in the current results; your edits are unsaved.", true);
            updateAgentContext();
        } else if (currentNoteId != null && currentNoteId.equals(lastSavedId)) {
            setStatus("Note saved.", false);
            updateAgentContext();
        } else {
            clearEditor();
            setStatus("Select a note or create a new one.", false);
        }
    }

    private void updateAgentContext() {
        if (notesAgentPanel != null) {
            notesAgentPanel.setContext(currentNote, dirty);
        }
        updateQuoteButton();
    }

    private void quoteSelection() {
        String text = selectedText();
        if (notesAgentPanel == null || currentNote == null
                || currentNote.getId() == null || currentNote.getId().isBlank()
                || text == null || text.isBlank()) {
            return;
        }
        notesAgentPanel.addQuote(currentNote.getTitle(), text);
        setStatus("Quoted selection added to the agent chat.", false);
    }

    private void updateQuoteButton() {
        boolean canQuote = notesAgentPanel != null
                && currentNote != null
                && currentNote.getId() != null
                && !currentNote.getId().isBlank()
                && selectedText() != null;
        quoteButton.setEnabled(canQuote);
    }

    private String selectedText() {
        if (hasSelection(lastSelectionSource)) {
            return lastSelectionSource.getSelectedText();
        }
        if (hasSelection(markdownArea)) {
            return markdownArea.getSelectedText();
        }
        if (hasSelection(previewPane)) {
            return previewPane.getSelectedText();
        }
        return null;
    }

    private static boolean hasSelection(JTextComponent component) {
        if (component == null
                || component.getSelectionStart() == component.getSelectionEnd()) {
            return false;
        }
        String text = component.getSelectedText();
        return text != null && !text.isBlank();
    }

    private void updatePreview() {
        Node document = parser.parse(markdownArea.getText());
        String body = renderer.render(document);
        previewPane.setText("<html><head><style>"
                + "body { font-family: sans-serif; font-size: 13px; color: #1e293b; }"
                + "h1, h2, h3 { color: #0f172a; }"
                + "code { background-color: #f1f5f9; }"
                + "pre { background-color: #f1f5f9; padding: 8px; }"
                + "blockquote { color: #475569; }"
                + "</style></head><body>" + body + "</body></html>");
        previewPane.setCaretPosition(0);
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
        statusLabel.setForeground(error ? ERROR : MUTED);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class NoteRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof Note) {
                Note note = (Note) value;
                String meta = note.getTags().isEmpty()
                        ? UPDATED_FORMAT.format(note.getUpdatedAt())
                        : escapeHtml(String.join(" · ", note.getTags()))
                        + " · " + UPDATED_FORMAT.format(note.getUpdatedAt());
                setText("<html><b>" + escapeHtml(note.getTitle()) + "</b><br>"
                        + "<font color='#64748b'>" + meta + "</font></html>");
                setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            }
            return this;
        }
    }
}
