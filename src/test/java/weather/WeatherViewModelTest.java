package weather;

import interface_adapter.weather.WeatherViewModel;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherViewModelTest {
    @Test
    void keepsItsOwnCopyOfBothTheRowsAndTheirCells() {
        List<String> row = new ArrayList<>(List.of("Sunday", "Nov 15", "5.4 °C", "Cloudy"));
        List<List<String>> rows = new ArrayList<>();
        rows.add(row);
        WeatherViewModel viewModel = new WeatherViewModel();

        viewModel.showForecast("Toronto", rows);
        row.set(3, "Changed outside the view model");
        rows.clear();

        assertEquals(List.of(List.of("Sunday", "Nov 15", "5.4 °C", "Cloudy")),
                viewModel.getRows());
    }

    @Test
    void preventsViewCodeFromChangingTheStoredForecast() {
        WeatherViewModel viewModel = new WeatherViewModel();
        viewModel.showForecast("Toronto", List.of(
                List.of("Sunday", "Nov 15", "5.4 °C", "Cloudy")));

        assertThrows(UnsupportedOperationException.class, () -> viewModel.getRows().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> viewModel.getRows().get(0).set(3, "Changed"));
    }
}