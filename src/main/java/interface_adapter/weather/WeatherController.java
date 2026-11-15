package interface_adapter.weather;

import use_case.weather.GetWeatherInputBoundary;
import use_case.weather.GetWeatherInputData;

public class WeatherController {
    private final GetWeatherInputBoundary inputBoundary;

    public WeatherController(GetWeatherInputBoundary inputBoundary) {
        this.inputBoundary = inputBoundary;
    }

    public void execute(String city, String province, String country) {
        inputBoundary.execute(new GetWeatherInputData(city, province, country));
    }
}