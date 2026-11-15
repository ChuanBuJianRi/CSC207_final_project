package view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Hosts the application's feature panels.
 */
public class MainFrame extends JFrame {
    private static final String WEATHER_PAGE = "WEATHER";
    private static final String AGENT_PAGE = "AGENT";

    public MainFrame(
            WeatherPanel weatherPanel,
            AgentPanel agentPanel) {
        super("Desktop Assistant");

        CardLayout cardLayout = new CardLayout();
        JPanel pages = new JPanel(cardLayout);
        pages.add(weatherPanel, WEATHER_PAGE);
        pages.add(agentPanel, AGENT_PAGE);

        JButton weatherButton = new JButton("Weather");
        JButton agentButton = new JButton("Agent");

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navigation.setBorder(
                BorderFactory.createEmptyBorder(4, 8, 4, 8));
        navigation.add(weatherButton);
        navigation.add(agentButton);

        weatherButton.addActionListener(
                event -> cardLayout.show(pages, WEATHER_PAGE));
        agentButton.addActionListener(
                event -> cardLayout.show(pages, AGENT_PAGE));

        JPanel root = new JPanel(new BorderLayout());
        root.add(navigation, BorderLayout.NORTH);
        root.add(pages, BorderLayout.CENTER);

        setContentPane(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 500));
        setSize(1100, 650);
        setLocationRelativeTo(null);

        cardLayout.show(pages, WEATHER_PAGE);
    }
}