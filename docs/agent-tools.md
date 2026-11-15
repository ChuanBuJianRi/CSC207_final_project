# Agent tools

## get_weather

Returns available daily forecasts for today and the next six days
at a specified location.

Dates follow the location's local timezone. Each day includes the
average temperature in Celsius and a weather description.

### Input parameters

| Parameter | Type | Required | Description | Example |
| --- | --- | --- | --- | --- |
| city | String | Yes | City name, without province or country | Toronto |
| province | String | No | Province or state; pass an empty string when omitted | Ontario or ON |
| country | String | Yes | Country name or country code | Canada or CA |

Leading and trailing whitespace is trimmed.

City and country must be nonblank. Each field must be provided
separately and must not contain commas.

Province/state location formats are supported for Canada, the
United States, and China. For other countries, leave province empty.

Examples:

- Toronto / Ontario / Canada
- Toronto / ON / CA
- Paris / empty province / France

No date parameter is required.

### Java input interface

Interface:

`use_case.weather.GetWeatherInputBoundary`

Method:

```java
void execute(GetWeatherInputData inputData);
```

Input class:

`use_case.weather.GetWeatherInputData`

Constructor:

```java
GetWeatherInputData(String city, String province, String country)
```

Example:

```java
weatherInputBoundary.execute(
        new GetWeatherInputData("Toronto", "Ontario", "Canada"));
```

Example without a province:

```java
weatherInputBoundary.execute(
        new GetWeatherInputData("Paris", "", "France"));
```

The Agent dispatcher invokes the weather use case through
GetWeatherInputBoundary.

### Java output interface

Interface:

`use_case.weather.GetWeatherOutputBoundary`

Methods:

```java
void prepareSuccessView(GetWeatherOutputData outputData);

void prepareFailView(String errorMessage);
```

The use case reports its outcome through this interface.

The Agent integration must capture the reported outcome before
claiming success. Returning from execute() alone does not mean
the query succeeded, because validation and service failures are
reported through prepareFailView().

### Successful output

Output class:

`use_case.weather.GetWeatherOutputData`

| Field | Java type | Description |
| --- | --- | --- |
| location | String | Resolved location returned by the weather service |
| days | List<GetWeatherOutputData.Day> | Available forecasts, ordered by date, up to seven days |

Each `GetWeatherOutputData.Day` contains:

| Field | Java type | Description |
| --- | --- | --- |
| date | LocalDate | Forecast date in the location's local timezone |
| temperature | double | Average temperature for the daily forecast period, in Celsius |
| condition | String | Weather description |

Accessors:

```java
outputData.getLocation();
outputData.getDays();

day.getDate();
day.getTemperature();
day.getCondition();
```

If the Agent needs a date string, LocalDate.toString() produces
ISO format: YYYY-MM-DD.

The provider may return fewer than seven days. This is a valid
partial result. The Agent should state the actual number of days
returned and must not invent missing forecasts.

### Failure cases

Failures are reported through:

```java
prepareFailView(String errorMessage);
```

Possible failures include:

- Missing or blank city.
- Missing or blank country.
- Commas used inside the separate location fields.
- Invalid or unsupported location.
- Missing API credentials.
- Authentication or subscription access denied.
- Request limit reached.
- Network failure or interrupted request.
- No forecast available within the requested seven-day window.
- Missing temperature or weather description.
- Invalid response data, timestamp, or timezone.

The current implementation provides forecasts only.
Historical weather queries are not implemented.

### Internal weather data access interface

Interface:

`use_case.weather.WeatherDataAccessInterface`

Method:

```java
List<WeatherReport> getForecast(
        String city,
        String province,
        String country);
```

Implementation:

`data_access.XweatherWeatherClient`

The implementation retrieves forecasts through one Xweather API
request and returns available days within the location's local
seven-day window.

Service failures are reported to the Interactor by throwing
IllegalStateException.

This interface is an internal dependency of the weather use case.
The Agent dispatcher uses GetWeatherInputBoundary as its entry point.

### View updates

WeatherPresenter implements GetWeatherOutputBoundary.

On success, it formats the forecast and updates WeatherViewModel.

On failure, it clears the previous forecast and stores the error
message in WeatherViewModel.

WeatherPanel observes ViewModel changes and updates Swing components
on the Event Dispatch Thread.

Agent integration must preserve the WeatherViewModel refresh and
also deliver the confirmed outcome to the Agent response.

### Confirmation

No confirmation is required for this read-only query.

### Configuration

The weather client reads these environment variables:

- XWEATHER_CLIENT_ID
- XWEATHER_CLIENT_SECRET

Credentials are application configuration, not tool arguments.
They must not be included in prompts, tool definitions, or responses.

### Integration status

The Weather use case and its input/output interfaces are implemented.

Registration of get_weather in the Agent's tools, argument mapping,
dispatch, and outcome handling must be completed by the Agent module.