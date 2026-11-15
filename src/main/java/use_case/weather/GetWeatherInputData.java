package use_case.weather;

/**
 * Input for a weather query.
 */
public class GetWeatherInputData {
    private final String city;
    private final String date;

    public GetWeatherInputData(String city, String date) {
        this.city = city;
        this.date = date;
    }

    public String getCity() {
        return city;
    }

    public String getDate() {
        return date;
    }
}