# Team Project

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.

## Weather

The Weather page queries Xweather daily forecasts by location and date.

Configure these environment variables in the environment used to run
`app.Main`:

- `XWEATHER_CLIENT_ID`
- `XWEATHER_CLIENT_SECRET`

The weather client reads environment variables directly and does not
automatically load `.env` files.

Example location: `toronto,on,ca`

Enter a date in `YYYY-MM-DD` format within the available forecast.
The result displays average temperature in Celsius and weather conditions.
Historical weather is not implemented.

The weather use case is available to the Agent module through
`GetWeatherInputBoundary`. Agent tool integration is pending.

Do not commit credentials.
