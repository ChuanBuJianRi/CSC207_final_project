package view;

import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentViewModel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Hashtable;

public class AgentPanel extends JPanel {
    private static final String[] MODELS = {"gpt-6.1-sol", "gemini-3.8-flash", "claude-opus-5-5"};
    private static final String[] EFFORTS = {"low", "medium", "high"};

    private final AgentController agentController;
    private final AgentViewModel agentViewModel;
    private final JComboBox<String> modelBox;
    private final JSlider effortSlider;
    private final JTextField input;
    private final JTextArea transcript;
    private final JButton sendButton;

    public AgentPanel(AgentController agentController, AgentViewModel agentViewModel) {
        this.agentController = agentController;
        this.agentViewModel = agentViewModel;
        this.modelBox = new JComboBox<>(MODELS);
        this.modelBox.setEditable(true);
        this.effortSlider = effortSlider();
        this.input = new JTextField(24);
        this.transcript = new JTextArea(16, 40);
        this.transcript.setEditable(false);
        this.transcript.setLineWrap(true);
        this.transcript.setWrapStyleWord(true);
        this.sendButton = new JButton("Send");

        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(modelBox);
        controls.add(effortSlider);
        controls.add(input);
        controls.add(sendButton);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(transcript), BorderLayout.CENTER);

        sendButton.addActionListener(event -> send());
        input.addActionListener(event -> send());
    }

    private void send() {
        String userMessage = input.getText();
        String model = String.valueOf(modelBox.getEditor().getItem()).trim();
        String effort = EFFORTS[effortSlider.getValue()];
        sendButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                agentController.execute(userMessage, model, effort);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    showResult(userMessage, model, effort);
                } catch (Exception exception) {
                    transcript.append(exception.getMessage() + "\n\n");
                }
                sendButton.setEnabled(true);
            }
        }.execute();
    }

    private JSlider effortSlider() {
        JSlider slider = new JSlider(0, EFFORTS.length - 1, 1);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(0, new JLabel("Low"));
        labels.put(1, new JLabel("Medium"));
        labels.put(2, new JLabel("High"));
        slider.setLabelTable(labels);
        return slider;
    }

    private void showResult(String userMessage, String model, String effort) {
        transcript.append("You (" + model + ", " + effort + "): " + userMessage + "\n");
        String errorMessage = agentViewModel.getErrorMessage();
        if (errorMessage != null && !errorMessage.isBlank()) {
            transcript.append(errorMessage + "\n\n");
            return;
        }
        transcript.append(agentViewModel.getResponse() + "\n\n");
        input.setText("");
    }
}
