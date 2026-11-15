package use_case.weather;

/**
 * Entry point for querying weather.
 */
public interface GetWeatherInputBoundary {
    void execute(GetWeatherInputData inputData);
}