# Agent tools
## get_weather

Queries the daily forecast for one location and date.

### Required inputs

| Parameter | Type | Format |
| --- | --- | --- |
| city | String | Xweather location, for example `toronto,on,ca` |
| date | String | ISO date: `YYYY-MM-DD` |

Both parameters must be nonblank.

### Java entry point

Interface: `use_case.weather.GetWeatherInputBoundary`

Input: `use_case.weather.GetWeatherInputData`

```java
weatherInputBoundary.execute(
        new GetWeatherInputData(city, date));
```

The dispatcher must invoke the weather use case rather than call
the HTTP client directly.

### Outcome

Results are reported through `GetWeatherOutputBoundary`.

- Success: `prepareSuccessView(GetWeatherOutputData outputData)`
- Failure: `prepareFailView(String errorMessage)`

Success fields:

- city
- date
- temperature: average temperature for the daily forecast period,
  in degrees Celsius
- condition: weather description

The agent integration must capture the reported outcome before
claiming success. Returning from `execute()` alone does not mean
the query succeeded.

### Failure cases

- Blank city or date
- Invalid date format or nonexistent date
- Location rejected by the provider
- Requested date absent from the returned forecast
- Missing credentials or denied API access
- Request limit reached
- Network failure
- Incomplete or invalid response

### View updates

The weather presenter updates `WeatherViewModel`.
`WeatherPanel` listens for changes and refreshes on the Swing EDT.
Agent integration should preserve this refresh behaviour.

### Confirmation

No confirmation is required for this read-only query.