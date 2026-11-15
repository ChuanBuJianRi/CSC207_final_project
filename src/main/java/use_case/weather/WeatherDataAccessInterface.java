package use_case.weather;

import entity.WeatherReport;
import java.time.LocalDate;

/**
 * Provides weather information from an external service.
 */
public interface WeatherDataAccessInterface {

    /**
     * Retrieves weather for the specified city and date.
     *
     * @param city the location to query
     * @param date the date to query
     * @return the weather report
     * @throws IllegalStateException if weather cannot be retrieved
     */
    WeatherReport getWeather(String city, LocalDate date);
}