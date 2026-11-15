package data_access;

import entity.WeatherReport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.weather.WeatherDataAccessInterface;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class XweatherWeatherClient implements WeatherDataAccessInterface {
    private static final String BASE_URL = "https://data.api.xweather.com/forecasts/";
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;

    public XweatherWeatherClient(String clientId, String clientSecret) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();
    }

    public static XweatherWeatherClient fromEnvironment() {
        return new XweatherWeatherClient(System.getenv("XWEATHER_CLIENT_ID"),
                System.getenv("XWEATHER_CLIENT_SECRET"));
    }

    @Override
    public List<WeatherReport> getForecast(String city, String province, String country) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Weather service credentials are not configured.");
        }

        String location = city + (province.isBlank() ? "" : "," + province) + "," + country;
        String url = BASE_URL + encode(location)
                + "?filter=day&limit=7"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json").GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 401 || status == 403) {
                throw new IllegalStateException("Weather service access was denied.");
            }
            if (status == 429) {
                throw new IllegalStateException("Request limit reached. Please try again later.");
            }
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Could not load the forecast. HTTP " + status + ".");
            }
            return parseForecast(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not connect. Check your internet and try again.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The weather request was interrupted.");
        }
    }

    private List<WeatherReport> parseForecast(String body) {
        try {
            JSONObject root = new JSONObject(body);
            if (!root.getBoolean("success")) {
                throw new IllegalStateException(
                        "Forecast unavailable. Check the location and weather service access.");
            }

            JSONArray results = root.getJSONArray("response");
            if (results.length() == 0) {
                throw new IllegalStateException("No forecast was found for this location.");
            }

            JSONObject result = results.getJSONObject(0);
            JSONObject place = result.getJSONObject("place");
            List<String> parts = new ArrayList<>();
            parts.add(place.getString("name"));
            String state = place.optString("state", "");
            if (!state.isBlank()) {
                parts.add(state.toUpperCase(Locale.ROOT));
            }
            parts.add(place.getString("country").toUpperCase(Locale.ROOT));
            String location = String.join(", ", parts);

            ZoneId zone = ZoneId.of(result.getJSONObject("profile").getString("tz"));
            LocalDate today = LocalDate.now(zone);
            JSONArray periods = result.getJSONArray("periods");
            Map<LocalDate, WeatherReport> days = new TreeMap<>();

            for (int index = 0; index < periods.length(); index++) {
                JSONObject period = periods.getJSONObject(index);
                LocalDate date = OffsetDateTime.parse(
                        period.getString("dateTimeISO")).toLocalDate();
                if (date.isBefore(today) || !date.isBefore(today.plusDays(7))) {
                    continue;
                }

                double temperature = period.getDouble("avgTempC");
                String condition = period.getString("weather");
                if (!Double.isFinite(temperature) || condition.isBlank()) {
                    throw new IllegalStateException("The forecast contains incomplete weather data.");
                }
                days.putIfAbsent(date, new WeatherReport(location, date, temperature, condition));
            }

            if (days.isEmpty()) {
                throw new IllegalStateException("No forecast is available for the next seven days.");
            }
            return new ArrayList<>(days.values());
        } catch (JSONException | DateTimeException exception) {
            throw new IllegalStateException("The weather service returned incomplete or invalid data.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}