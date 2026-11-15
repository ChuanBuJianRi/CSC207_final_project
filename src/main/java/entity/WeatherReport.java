package entity;

import java.time.LocalDate;

/**
 * Weather information for a city on a specific date.
 */
public class WeatherReport {
    private final String city;
    private final LocalDate date;
    private final double temperature;
    private final String condition;

    public WeatherReport(String city, LocalDate date,
                         double temperature, String condition) {
        this.city = city;
        this.date = date;
        this.temperature = temperature;
        this.condition = condition;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the temperature in degrees Celsius.
     */
    public double getTemperature() {
        return temperature;
    }

    public String getCondition() {
        return condition;
    }
}