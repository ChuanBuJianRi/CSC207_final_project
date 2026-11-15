package use_case.weather;

/**
 * Reports the outcome of a weather query.
 */
public interface GetWeatherOutputBoundary {
    void prepareSuccessView(GetWeatherOutputData outputData);

    void prepareFailView(String errorMessage);
}