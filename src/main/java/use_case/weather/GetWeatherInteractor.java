package use_case.weather;

import entity.WeatherReport;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Validates input and coordinates a weather query.
 */
public class GetWeatherInteractor implements GetWeatherInputBoundary {
    private final WeatherDataAccessInterface weatherDataAccess;
    private final GetWeatherOutputBoundary outputBoundary;

    public GetWeatherInteractor(
            WeatherDataAccessInterface weatherDataAccess,
            GetWeatherOutputBoundary outputBoundary) {
        this.weatherDataAccess = weatherDataAccess;
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute(GetWeatherInputData inputData) {
        String city = inputData.getCity() == null
                ? "" : inputData.getCity().trim();
        String dateText = inputData.getDate() == null
                ? "" : inputData.getDate().trim();

        if (city.isEmpty()) {
            outputBoundary.prepareFailView("Enter a city.");
            return;
        }

        if (dateText.isEmpty()) {
            outputBoundary.prepareFailView("Enter a date.");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            outputBoundary.prepareFailView(
                    "Enter a valid date in YYYY-MM-DD format.");
            return;
        }

        WeatherReport report;
        try {
            report = weatherDataAccess.getWeather(city, date);
        } catch (IllegalStateException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "Could not retrieve weather.";
            }
            outputBoundary.prepareFailView(message);
            return;
        }

        GetWeatherOutputData outputData = new GetWeatherOutputData(
                report.getCity(),
                report.getDate(),
                report.getTemperature(),
                report.getCondition());

        outputBoundary.prepareSuccessView(outputData);
    }
}