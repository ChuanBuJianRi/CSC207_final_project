package use_case.weather;

import java.time.LocalDate;

/**
 * Output of a successful weather query.
 */
public class GetWeatherOutputData {
    private final String city;
    private final LocalDate date;
    private final double temperature;
    private final String condition;

    public GetWeatherOutputData(String city, LocalDate date,
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

    public double getTemperature() {
        return temperature;
    }

    public String getCondition() {
        return condition;
    }
}