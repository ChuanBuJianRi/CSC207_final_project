package view;

import interface_adapter.weather.WeatherController;
import interface_adapter.weather.WeatherViewModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class WeatherPanel extends JPanel {
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Color LINE = new Color(226, 232, 240);

    private final WeatherController controller;
    private final WeatherViewModel viewModel;
    private final JTextField cityInput = new JTextField();
    private final JTextField provinceInput = new JTextField();
    private final JTextField countryInput = new JTextField();
    private final JButton searchButton = new JButton("Show 7-day forecast");
    private final JLabel locationLabel = new JLabel("The week ahead");
    private final JLabel detailLabel = new JLabel("Daily averages in Celsius · Local forecast dates");
    private final JLabel statusLabel = new JLabel("Enter a location to get started.");
    private final JLabel emptyLabel = new JLabel("Search for a city to see its forecast.", SwingConstants.CENTER);
    private final CardLayout resultLayout = new CardLayout();
    private final JPanel resultPages = new JPanel(resultLayout);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Day", "Date", "Average temperature", "Weather"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public WeatherPanel(WeatherController controller, WeatherViewModel viewModel) {
        this.controller = controller;
        this.viewModel = viewModel;
        setLayout(new BorderLayout(0, 20));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        heading.add(label("Weather", 28, Font.BOLD, TEXT), BorderLayout.NORTH);
        heading.add(label("Find your city. See the next seven days.", 14, Font.PLAIN, MUTED),
                BorderLayout.CENTER);

        JPanel fields = new JPanel(new GridLayout(1, 3, 16, 0));
        fields.setOpaque(false);
        fields.add(field("City", "e.g. Toronto", cityInput));
        fields.add(field("Province / State (optional)", "e.g. Ontario or ON", provinceInput));
        fields.add(field("Country", "e.g. Canada or CA", countryInput));
        provinceInput.setToolTipText(
                "Supported for Canada, the United States, and China. Otherwise leave blank.");

        searchButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        searchButton.setBackground(ACCENT);
        searchButton.setForeground(Color.WHITE);
        searchButton.setOpaque(true);
        searchButton.setBorderPainted(false);
        searchButton.setPreferredSize(new Dimension(205, 40));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setOpaque(false);
        actions.add(label("Forecast for today and the next six days.", 12, Font.PLAIN, MUTED),
                BorderLayout.CENTER);
        actions.add(searchButton, BorderLayout.EAST);

        JPanel form = card();
        form.setLayout(new BorderLayout(0, 16));
        form.add(fields, BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 20));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        locationLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        locationLabel.setForeground(TEXT);
        detailLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        detailLabel.setForeground(MUTED);
        JPanel resultHeading = new JPanel(new BorderLayout(0, 6));
        resultHeading.setOpaque(false);
        resultHeading.add(locationLabel, BorderLayout.NORTH);
        resultHeading.add(detailLabel, BorderLayout.CENTER);

        JTable table = new JTable(tableModel);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(390);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable owner, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(owner, value, false, false, row, column);
                setBackground(row % 2 == 0 ? Color.WHITE : BACKGROUND);
                setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });

        DefaultTableCellRenderer header = new DefaultTableCellRenderer();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.setForeground(MUTED);
        header.setBackground(BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
        table.getTableHeader().setDefaultRenderer(header);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(Color.WHITE);
        resultPages.setBackground(Color.WHITE);
        emptyLabel.setForeground(MUTED);
        emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        resultPages.add(emptyLabel, "EMPTY");
        resultPages.add(scroll, "FORECAST");

        JPanel results = card();
        results.setLayout(new BorderLayout(0, 16));
        results.add(resultHeading, BorderLayout.NORTH);
        results.add(resultPages, BorderLayout.CENTER);
        add(results, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(label("Weather data: Xweather", 12, Font.PLAIN, MUTED), BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        cityInput.addActionListener(event -> search());
        provinceInput.addActionListener(event -> search());
        countryInput.addActionListener(event -> search());
        viewModel.addPropertyChangeListener(event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                render();
            } else {
                SwingUtilities.invokeLater(this::render);
            }
        });
    }

    private void search() {
        if (!searchButton.isEnabled()) {
            return;
        }
        String city = cityInput.getText();
        String province = provinceInput.getText();
        String country = countryInput.getText();
        setBusy(true);
        tableModel.setRowCount(0);
        locationLabel.setText("The week ahead");
        detailLabel.setText("Daily averages in Celsius · Local forecast dates");
        emptyLabel.setText("Loading your forecast...");
        resultLayout.show(resultPages, "EMPTY");
        statusLabel.setForeground(MUTED);
        statusLabel.setText("Looking up your location...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                controller.execute(city, province, country);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The search was interrupted. Please try again.");
                } catch (ExecutionException exception) {
                    showError("Could not load the forecast. Please try again.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void render() {
        if (!viewModel.getErrorMessage().isBlank()) {
            showError(viewModel.getErrorMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (List<String> row : viewModel.getRows()) {
            tableModel.addRow(row.toArray());
        }
        locationLabel.setText(viewModel.getLocation());
        detailLabel.setText("Daily averages in Celsius · Dates are local to this location");
        statusLabel.setForeground(MUTED);
        int count = viewModel.getRows().size();
        statusLabel.setText(count == 7 ? "Your 7-day forecast is ready."
                : "Only " + count + " of 7 days are available from the weather service.");
        resultLayout.show(resultPages, "FORECAST");
    }

    private void showError(String message) {
        tableModel.setRowCount(0);
        emptyLabel.setText("Please check your search and try again.");
        resultLayout.show(resultPages, "EMPTY");
        statusLabel.setForeground(new Color(185, 28, 28));
        statusLabel.setText(message);
    }

    private void setBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        searchButton.setText(busy ? "Loading..." : "Show 7-day forecast");
        cityInput.setEnabled(!busy);
        provinceInput.setEnabled(!busy);
        countryInput.setEnabled(!busy);
    }

    private static JPanel field(String title, String example, JTextField input) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        input.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        input.setPreferredSize(new Dimension(160, 40));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        input.getAccessibleContext().setAccessibleName(title);
        panel.add(label(title, 13, Font.BOLD, TEXT), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        panel.add(label(example, 12, Font.PLAIN, MUTED), BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return panel;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        label.setForeground(color);
        return label;
    }
}