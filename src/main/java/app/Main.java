package app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new AppBuilder().build().setVisible(true);
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(null,
                        "Could not start the application:\n" + exception.getMessage(),
                        "Desktop Assistant", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
