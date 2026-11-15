package use_case.weather;

public class GetWeatherInputData {
    private final String city;
    private final String province;
    private final String country;

    public GetWeatherInputData(String city, String province, String country) {
        this.city = city;
        this.province = province;
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getCountry() {
        return country;
    }
}