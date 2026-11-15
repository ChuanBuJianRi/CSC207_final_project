package interface_adapter.weather;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.stream.Collectors;

public class WeatherViewModel {
    private String location = "";
    private List<List<String>> rows = List.of();
    private String errorMessage = "";
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public String getLocation() {
        return location;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void showForecast(String location, List<List<String>> rows) {
        this.location = location;
        this.rows = List.copyOf(rows.stream()
                .map(row -> List.copyOf(row))
                .collect(Collectors.toList()));
        this.errorMessage = "";
        support.firePropertyChange("weather", null, null);
    }

    public void showError(String message) {
        location = "";
        rows = List.of();
        errorMessage = message;
        support.firePropertyChange("weather", null, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}