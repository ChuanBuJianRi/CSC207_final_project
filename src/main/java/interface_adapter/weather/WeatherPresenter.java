package interface_adapter.weather;

import use_case.weather.GetWeatherOutputBoundary;
import use_case.weather.GetWeatherOutputData;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherPresenter implements GetWeatherOutputBoundary {
    private final WeatherViewModel viewModel;

    public WeatherPresenter(WeatherViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(GetWeatherOutputData outputData) {
        DateTimeFormatter weekday = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        List<List<String>> rows = new ArrayList<>();

        for (GetWeatherOutputData.Day day : outputData.getDays()) {
            rows.add(List.of(
                    day.getDate().format(weekday),
                    day.getDate().format(date),
                    String.format(Locale.ROOT, "%.1f °C", day.getTemperature()),
                    day.getCondition()));
        }

        viewModel.showForecast(outputData.getLocation(), rows);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}