package view;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {
    public MainFrame(AgentPanel agentPanel, TodoPanel todoPanel) {
        super("Desktop Assistant");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Agent", agentPanel);
        tabs.addTab("Todo", todoPanel);
        setContentPane(tabs);
        pack();
        setLocationRelativeTo(null);
    }
}
