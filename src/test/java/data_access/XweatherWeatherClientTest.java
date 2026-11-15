package data_access;

import com.sun.net.httpserver.HttpServer;
import entity.WeatherReport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class XweatherWeatherClientTest {
    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-15T02:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    private HttpServer server;
    private String baseUrl;
    private XweatherWeatherClient client;
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicReference<URI> requestUri = new AtomicReference<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        responseBody.set(response(periods(7)).toString());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/forecasts/", exchange -> {
            requestUri.set(exchange.getRequestURI());
            requestMethod.set(exchange.getRequestMethod());
            requestCount.incrementAndGet();
            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/forecasts/";
        client = configuredClient("test-id", "test-secret");
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestsSevenDaysOnceAndParsesTheResponse() {
        List<WeatherReport> forecast = client.getForecast("Toronto", "Ontario", "Canada");

        assertEquals(1, requestCount.get());
        assertEquals("GET", requestMethod.get());
        assertEquals("/forecasts/Toronto,Ontario,Canada", requestUri.get().getPath());
        Map<String, String> query = queryParameters(requestUri.get());
        assertEquals("day", query.get("filter"));
        assertEquals("7", query.get("limit"));
        assertEquals("test-id", query.get("client_id"));
        assertEquals("test-secret", query.get("client_secret"));
        assertEquals(7, forecast.size());
        assertEquals("Toronto, ON, CA", forecast.get(0).getCity());
        for (int index = 0; index < 7; index++) {
            assertEquals(TODAY.plusDays(index), forecast.get(index).getDate());
            assertEquals(5.4 + index, forecast.get(index).getTemperature(), 0.001);
            assertEquals("Partly Cloudy", forecast.get(index).getCondition());
        }
    }

    @Test
    void omitsTheProvinceSeparatorWhenProvinceIsEmpty() {
        client.getForecast("Paris", "", "France");

        assertEquals("/forecasts/Paris,France", requestUri.get().getPath());
        assertEquals(1, requestCount.get());
    }

    @Test
    void encodesSpacesInLocationFields() {
        client.getForecast("New York", "New York", "United States");

        assertEquals("/forecasts/New York,New York,United States", requestUri.get().getPath());
        assertTrue(requestUri.get().getRawPath().contains("%20"));
    }

    @Test
    void acceptsABaseUrlWithoutATrailingSlash() {
        XweatherWeatherClient alternate = new XweatherWeatherClient(
                "test-id", "test-secret", baseUrl.substring(0, baseUrl.length() - 1), HTTP, CLOCK);

        assertEquals(7, alternate.getForecast("Toronto", "Ontario", "Canada").size());
    }

    @Test
    void usesTheLocationDateRatherThanTheUtcDate() {
        responseBody.set(response(new JSONArray().put(period(TODAY, 5.4))).toString());

        List<WeatherReport> forecast = client.getForecast("Toronto", "Ontario", "Canada");

        assertEquals(LocalDate.of(2026, 11, 15), LocalDate.now(CLOCK));
        assertEquals(LocalDate.of(2026, 11, 14), forecast.get(0).getDate());
    }

    @Test
    void sortsDatesRemovesDuplicatesAndExcludesDatesOutsideTheWeek() {
        JSONArray periods = new JSONArray();
        periods.put(period(TODAY.plusDays(3), 3.0));
        periods.put(period(TODAY.minusDays(1), -99.0));
        periods.put(period(TODAY.plusDays(7), 99.0));
        periods.put(period(TODAY.plusDays(3), 33.0));
        for (int index : new int[]{6, 0, 5, 1, 4, 2}) {
            periods.put(period(TODAY.plusDays(index), index));
        }
        responseBody.set(response(periods).toString());

        List<WeatherReport> forecast = client.getForecast("Toronto", "Ontario", "Canada");

        assertEquals(7, forecast.size());
        for (int index = 0; index < 7; index++) {
            assertEquals(TODAY.plusDays(index), forecast.get(index).getDate());
            assertEquals(index, forecast.get(index).getTemperature(), 0.001);
        }
    }

    @Test
    void returnsOnlyAvailableDaysWhenTheProviderHasAPartialForecast() {
        responseBody.set(response(periods(3)).toString());

        assertEquals(3, client.getForecast("Toronto", "Ontario", "Canada").size());
    }

    @Test
    void allowsAResponseWithNoProvince() {
        JSONObject body = response(periods(1));
        JSONObject place = body.getJSONArray("response").getJSONObject(0).getJSONObject("place");
        place.put("name", "Paris");
        place.put("state", JSONObject.NULL);
        place.put("country", "fr");
        responseBody.set(body.toString());

        assertEquals("Paris, FR", client.getForecast("Paris", "", "France").get(0).getCity());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsMissingClientIdWithoutSendingARequest(String id) {
        client = configuredClient(id, "test-secret");

        assertTrue(failure().getMessage().contains("credentials"));
        assertEquals(0, requestCount.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsMissingSecretWithoutSendingARequest(String secret) {
        client = configuredClient("test-id", secret);

        assertTrue(failure().getMessage().contains("credentials"));
        assertEquals(0, requestCount.get());
    }

    @ParameterizedTest
    @CsvSource({
            "401, Weather service access was denied.",
            "403, Weather service access was denied.",
            "429, Request limit reached. Please try again later.",
            "400, Could not load the forecast. HTTP 400.",
            "500, Could not load the forecast. HTTP 500.",
            "503, Could not load the forecast. HTTP 503."
    })
    void reportsHttpErrors(int status, String message) {
        responseStatus.set(status);
        responseBody.set("not a JSON success response");

        assertEquals(message, failure().getMessage());
        assertEquals(1, requestCount.get());
    }

    @Test
    void reportsAProviderFailureEvenWhenHttpStatusIs200() {
        responseBody.set(new JSONObject().put("success", false)
                .put("error", new JSONObject().put("code", "invalid_location")
                        .put("description", "Invalid location; test-secret"))
                .toString());

        String message = failure().getMessage();

        assertTrue(message.contains("Forecast unavailable"));
        assertFalse(message.contains("test-secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "[]", "{}", "{\"success\":true,\"response\":null}"})
    void reportsMalformedOrUnexpectedResponses(String body) {
        responseBody.set(body);

        assertEquals("The weather service returned incomplete or invalid data.",
                failure().getMessage());
    }

    @Test
    void reportsAnEmptyLocationResult() {
        responseBody.set(new JSONObject().put("success", true)
                .put("response", new JSONArray()).toString());

        assertEquals("No forecast was found for this location.", failure().getMessage());
    }

    @Test
    void reportsNoForecastPeriods() {
        responseBody.set(response(new JSONArray()).toString());

        assertEquals("No forecast is available for the next seven days.", failure().getMessage());
    }

    @Test
    void rejectsAMissingTemperatureInsteadOfDisplayingZero() {
        JSONObject day = period(TODAY, 5.4);
        day.put("avgTempC", JSONObject.NULL);
        responseBody.set(response(new JSONArray().put(day)).toString());

        assertEquals("The weather service returned incomplete or invalid data.",
                failure().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
    void rejectsNonFiniteTemperatures(String temperature) {
        JSONObject day = period(TODAY, 5.4).put("avgTempC", temperature);
        responseBody.set(response(new JSONArray().put(day)).toString());

        assertEquals("The forecast contains incomplete weather data.", failure().getMessage());
    }

    @Test
    void rejectsAnEmptyWeatherDescription() {
        JSONObject day = period(TODAY, 5.4).put("weather", " ");
        responseBody.set(response(new JSONArray().put(day)).toString());

        assertEquals("The forecast contains incomplete weather data.", failure().getMessage());
    }

    @Test
    void reportsAnInvalidForecastTimestamp() {
        JSONObject day = period(TODAY, 5.4).put("dateTimeISO", "invalid-date");
        responseBody.set(response(new JSONArray().put(day)).toString());

        assertEquals("The weather service returned incomplete or invalid data.",
                failure().getMessage());
    }

    @Test
    void reportsAnInvalidLocationTimezone() {
        JSONObject body = response(periods(1));
        body.getJSONArray("response").getJSONObject(0).getJSONObject("profile")
                .put("tz", "Invalid/Timezone");
        responseBody.set(body.toString());

        assertEquals("The weather service returned incomplete or invalid data.",
                failure().getMessage());
    }

    @Test
    void reportsConnectionFailures() {
        server.stop(0);
        server = null;

        assertEquals("Could not connect. Check your internet and try again.",
                failure().getMessage());
    }

    @Test
    void preservesTheThreadInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            assertEquals("The weather request was interrupted.", failure().getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private XweatherWeatherClient configuredClient(String id, String secret) {
        return new XweatherWeatherClient(id, secret, baseUrl, HTTP, CLOCK);
    }

    private IllegalStateException failure() {
        return assertThrows(IllegalStateException.class,
                () -> client.getForecast("Toronto", "Ontario", "Canada"));
    }

    private static JSONArray periods(int count) {
        JSONArray periods = new JSONArray();
        for (int index = 0; index < count; index++) {
            periods.put(period(TODAY.plusDays(index), 5.4 + index));
        }
        return periods;
    }

    private static JSONObject period(LocalDate date, double temperature) {
        return new JSONObject()
                .put("dateTimeISO", date.atTime(7, 0).atZone(TORONTO)
                        .toOffsetDateTime().toString())
                .put("avgTempC", temperature)
                .put("weather", "Partly Cloudy");
    }

    private static JSONObject response(JSONArray periods) {
        return new JSONObject().put("success", true).put("response", new JSONArray()
                .put(new JSONObject()
                        .put("place", new JSONObject().put("name", "Toronto")
                                .put("state", "on").put("country", "ca"))
                        .put("profile", new JSONObject().put("tz", "America/Toronto"))
                        .put("periods", periods)));
    }

    private static Map<String, String> queryParameters(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1])));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}