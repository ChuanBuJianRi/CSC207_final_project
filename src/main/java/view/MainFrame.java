package view;

import javax.swing.JFrame;

public class MainFrame extends JFrame {
    public MainFrame(AgentPanel agentPanel) {
        super("Desktop Assistant");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(agentPanel);
        pack();
        setLocationRelativeTo(null);
    }
}
