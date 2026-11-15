package use_case.weather;

import java.time.LocalDate;
import java.util.List;

public class GetWeatherOutputData {
    private final String location;
    private final List<Day> days;

    public GetWeatherOutputData(String location, List<Day> days) {
        this.location = location;
        this.days = List.copyOf(days);
    }

    public String getLocation() {
        return location;
    }

    public List<Day> getDays() {
        return days;
    }

    public static final class Day {
        private final LocalDate date;
        private final double temperature;
        private final String condition;

        public Day(LocalDate date, double temperature, String condition) {
            this.date = date;
            this.temperature = temperature;
            this.condition = condition;
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
}