package weather;

import interface_adapter.weather.WeatherPresenter;
import interface_adapter.weather.WeatherViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import use_case.weather.GetWeatherOutputData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherPresenterTest {
    private WeatherViewModel viewModel;
    private WeatherPresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new WeatherViewModel();
        presenter = new WeatherPresenter(viewModel);
    }

    @Test
    void formatsForecastForTheEnglishCelsiusInterface() {
        presenter.prepareSuccessView(forecast("Toronto, ON, CA", 5.4));

        assertEquals("Toronto, ON, CA", viewModel.getLocation());
        assertEquals(List.of(List.of("Sunday", "Nov 15", "5.4 °C", "Partly Cloudy")),
                viewModel.getRows());
        assertEquals("", viewModel.getErrorMessage());
    }

    @Test
    void formatsConsistentlyWhenSystemLocaleUsesDecimalCommas() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);

            presenter.prepareSuccessView(forecast("Toronto", -2.25));

            assertEquals(List.of("Sunday", "Nov 15", "-2.3 °C", "Partly Cloudy"),
                    viewModel.getRows().get(0));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void clearsOldErrorsAfterASuccessfulRetry() {
        presenter.prepareFailView("Network unavailable.");

        presenter.prepareSuccessView(forecast("Toronto", 5.4));

        assertEquals("", viewModel.getErrorMessage());
        assertEquals(1, viewModel.getRows().size());
        assertEquals("Toronto", viewModel.getLocation());
    }

    @Test
    void clearsPreviousLocationAndForecastAfterFailure() {
        presenter.prepareSuccessView(forecast("Toronto", 5.4));

        presenter.prepareFailView("Location not found.");

        assertEquals("Location not found.", viewModel.getErrorMessage());
        assertTrue(viewModel.getRows().isEmpty());
        assertEquals("", viewModel.getLocation());
    }

    @Test
    void replacesThePreviousSearchInsteadOfAppendingRows() {
        presenter.prepareSuccessView(forecast("Toronto", 5.4));

        presenter.prepareSuccessView(forecast("Paris", 12.6));

        assertEquals("Paris", viewModel.getLocation());
        assertEquals(1, viewModel.getRows().size());
        assertEquals("12.6 °C", viewModel.getRows().get(0).get(2));
    }

    @Test
    void notifiesTheViewWithCompleteStateForSuccessAndFailure() {
        List<String> observed = new ArrayList<>();
        viewModel.addPropertyChangeListener(event -> observed.add(
                viewModel.getLocation() + "|" + viewModel.getRows().size()
                        + "|" + viewModel.getErrorMessage()));

        presenter.prepareSuccessView(forecast("Toronto", 5.4));
        presenter.prepareFailView("Offline.");

        assertEquals(List.of("Toronto|1|", "|0|Offline."), observed);
    }

    private static GetWeatherOutputData forecast(String location, double temperature) {
        return new GetWeatherOutputData(location, List.of(new GetWeatherOutputData.Day(
                LocalDate.of(2026, 11, 15), temperature, "Partly Cloudy")));
    }
}