package use_case.weather;

import entity.WeatherReport;
import java.util.List;

public interface WeatherDataAccessInterface {
    /**
     * Returns available daily forecasts for the next seven local dates.
     * Throws IllegalStateException when the request cannot be completed.
     */
    List<WeatherReport> getForecast(String city, String province, String country);
}