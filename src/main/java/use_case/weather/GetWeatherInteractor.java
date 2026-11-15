package use_case.weather;

import entity.WeatherReport;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GetWeatherInteractor implements GetWeatherInputBoundary {
    private final WeatherDataAccessInterface weatherDataAccess;
    private final GetWeatherOutputBoundary outputBoundary;

    public GetWeatherInteractor(WeatherDataAccessInterface weatherDataAccess,
                                GetWeatherOutputBoundary outputBoundary) {
        this.weatherDataAccess = weatherDataAccess;
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute(GetWeatherInputData inputData) {
        String city = clean(inputData.getCity());
        String province = clean(inputData.getProvince());
        String country = clean(inputData.getCountry());

        if (city.isEmpty()) {
            outputBoundary.prepareFailView("Enter a city, for example Toronto.");
            return;
        }
        if (country.isEmpty()) {
            outputBoundary.prepareFailView("Enter a country, for example Canada.");
            return;
        }
        if (city.contains(",") || province.contains(",") || country.contains(",")) {
            outputBoundary.prepareFailView(
                    "Enter city, province/state, and country in separate fields.");
            return;
        }

        List<WeatherReport> reports;
        try {
            reports = weatherDataAccess.getForecast(city, province, country);
        } catch (IllegalStateException exception) {
            String message = exception.getMessage();
            outputBoundary.prepareFailView(message == null || message.isBlank()
                    ? "Could not load the forecast. Please try again." : message);
            return;
        }

        if (reports == null || reports.isEmpty()) {
            outputBoundary.prepareFailView("No forecast is available for this location.");
            return;
        }

        List<GetWeatherOutputData.Day> days = reports.stream()
                .sorted(Comparator.comparing(WeatherReport::getDate))
                .limit(7)
                .map(report -> new GetWeatherOutputData.Day(
                        report.getDate(), report.getTemperature(), report.getCondition()))
                .collect(Collectors.toList());

        outputBoundary.prepareSuccessView(
                new GetWeatherOutputData(reports.get(0).getCity(), days));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}