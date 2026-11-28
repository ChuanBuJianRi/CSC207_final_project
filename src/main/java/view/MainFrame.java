package view;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.Dimension;

public class MainFrame extends JFrame {
    public MainFrame(AgentPanel agentPanel, TodoPanel todoPanel, NotesPanel notesPanel) {
        super("Desktop Assistant");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Agent", agentPanel);
        tabs.addTab("Todo", todoPanel);
        tabs.addTab("Notes", notesPanel);
        setContentPane(tabs);
        pack();
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
    }
}
