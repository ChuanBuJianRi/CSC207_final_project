package weather;

import entity.WeatherReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.weather.GetWeatherInputData;
import use_case.weather.GetWeatherInteractor;
import use_case.weather.GetWeatherOutputBoundary;
import use_case.weather.GetWeatherOutputData;
import use_case.weather.WeatherDataAccessInterface;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GetWeatherInteractorTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 15);
    private FakeWeatherSource source;
    private RecordingOutput output;
    private GetWeatherInteractor interactor;

    @BeforeEach
    void setUp() {
        source = new FakeWeatherSource();
        output = new RecordingOutput();
        interactor = new GetWeatherInteractor(source, output);
    }

    @Test
    void returnsSevenDaysAndPreservesWeatherValues() {
        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        assertEquals(1, source.calls);
        assertEquals(1, output.successCalls);
        assertEquals(0, output.failureCalls);
        assertEquals("Toronto, ON, CA", output.success.getLocation());
        assertEquals(7, output.success.getDays().size());
        for (int index = 0; index < 7; index++) {
            GetWeatherOutputData.Day day = output.success.getDays().get(index);
            assertEquals(START.plusDays(index), day.getDate());
            assertEquals(-2.5 + index, day.getTemperature(), 0.001);
            assertEquals("Cloudy", day.getCondition());
        }
    }

    @Test
    void trimsLocationFieldsBeforeQuerying() {
        interactor.execute(new GetWeatherInputData(" Toronto ", " Ontario ", " Canada "));

        assertEquals("Toronto", source.city);
        assertEquals("Ontario", source.province);
        assertEquals("Canada", source.country);
        assertEquals(1, output.successCalls);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void acceptsOmittedProvince(String province) {
        interactor.execute(new GetWeatherInputData("Paris", province, "France"));

        assertEquals("", source.province);
        assertEquals(1, source.calls);
        assertEquals(1, output.successCalls);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingCityBeforeCallingService(String city) {
        interactor.execute(new GetWeatherInputData(city, "Ontario", "Canada"));

        assertFailure("Enter a city, for example Toronto.");
        assertEquals(0, source.calls);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingCountryBeforeCallingService(String country) {
        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", country));

        assertFailure("Enter a country, for example Canada.");
        assertEquals(0, source.calls);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Toronto,ON | Ontario | Canada",
            "Toronto | Ontario,ON | Canada",
            "Toronto | Ontario | Canada,CA"
    })
    void requiresSeparateLocationFields(String city, String province, String country) {
        interactor.execute(new GetWeatherInputData(city, province, country));

        assertFailure("Enter city, province/state, and country in separate fields.");
        assertEquals(0, source.calls);
    }

    @Test
    void ordersForecastByDateAndLimitsOutputToSevenDays() {
        source.reports = reports(9);
        Collections.reverse(source.reports);

        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        assertEquals(7, output.success.getDays().size());
        for (int index = 0; index < 7; index++) {
            assertEquals(START.plusDays(index), output.success.getDays().get(index).getDate());
        }
    }

    @Test
    void returnsAvailableDaysWithoutInventingMissingForecasts() {
        source.reports = reports(3);

        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        assertEquals(3, output.success.getDays().size());
        assertEquals(1, output.successCalls);
        assertEquals(0, output.failureCalls);
    }

    @Test
    void reportsAnEmptyForecastAsFailure() {
        source.reports = List.of();

        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        assertFailure("No forecast is available for this location.");
    }

    @Test
    void reportsANullForecastAsFailure() {
        source.reports = null;

        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        assertFailure("No forecast is available for this location.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Unknown city.", "Service unavailable."})
    void reportsServiceFailuresWithoutReportingSuccess(String message) {
        source.failure = new IllegalStateException(message);

        interactor.execute(new GetWeatherInputData("Toronto", "Ontario", "Canada"));

        String expected = message == null || message.isBlank()
                ? "Could not load the forecast. Please try again." : message;
        assertFailure(expected);
        assertEquals(1, source.calls);
    }

    private void assertFailure(String expected) {
        assertEquals(expected, output.failure);
        assertEquals(1, output.failureCalls);
        assertEquals(0, output.successCalls);
        assertNull(output.success);
    }

    private static List<WeatherReport> reports(int count) {
        List<WeatherReport> reports = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            reports.add(new WeatherReport("Toronto, ON, CA", START.plusDays(index),
                    -2.5 + index, "Cloudy"));
        }
        return reports;
    }

    private static final class FakeWeatherSource implements WeatherDataAccessInterface {
        private List<WeatherReport> reports = GetWeatherInteractorTest.reports(7);
        private IllegalStateException failure;
        private String city;
        private String province;
        private String country;
        private int calls;

        @Override
        public List<WeatherReport> getForecast(String city, String province, String country) {
            calls++;
            this.city = city;
            this.province = province;
            this.country = country;
            if (failure != null) {
                throw failure;
            }
            return reports;
        }
    }

    private static final class RecordingOutput implements GetWeatherOutputBoundary {
        private GetWeatherOutputData success;
        private String failure;
        private int successCalls;
        private int failureCalls;

        @Override
        public void prepareSuccessView(GetWeatherOutputData outputData) {
            successCalls++;
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failureCalls++;
            failure = errorMessage;
        }
    }
}