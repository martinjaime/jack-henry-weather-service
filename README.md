# Jack Henry Assessment: Weather API

Small HTTP service that returns the current forecast for a latitude/longitude
using the National Weather Service API. No API key is required as of 2024-06.

## What It Returns

`GET /current-forecast?lat=<double>&lon=<double>`

Response:

- `condition`: short forecast text (for example, `"Partly Cloudy"`)
- `feelsLike`: one of `"hot"`, `"moderate"`, or `"cold"`

## Prerequisites

- JDK 21
- sbt 1.10.11

## How To Run

Can set the following environment variables to override defaults:

```sh
export HOST=localhost
export PORT=3000
export WEATHER_FEELS_LIKE_HOT_THRESHOLD_F=80
export WEATHER_FEELS_LIKE_COLD_THRESHOLD_F=50
```

Run with 

```sh
sbt run
```

Or run tests with

```sh
sbt test
```

Then make a request to the endpoint:

```sh
curl "http://localhost:3000/current-forecast?lat=38.9717&lon=-95.2353"
```

Example success response:

```json
{
  "condition": "Partly Cloudy",
  "feelsLike": "moderate"
}
```

## Error Behavior

The endpoint returns:

- `400` for bad request/domain validation issues (for example, unsupported temperature unit from upstream)
- `500` for unexpected server or upstream failures

Error body shape:

```json
{
  "error": "Internal server error"
}
```

## Configuration

The app reads configuration from `src/main/resources/application.conf` with optional environment variable overrides.

- `HOST` (default `localhost`)
- `PORT` (default `3000`)
- `WEATHER_FEELS_LIKE_HOT_THRESHOLD_F` (default `75`)
- `WEATHER_FEELS_LIKE_COLD_THRESHOLD_F` (default `60`)

## Test

```sh
sbt test
```

## Notes / Shortcuts

Implementation intentionally takes shortcuts for the sake of the assessment,
including but not limited to:

- Basic 200/400/500 error handling only
- Temperature classification logic lives in the route layer rather than a
  dedicated service or domain layer
- Limited test coverage focused on endpoint behavior, but does test temp
  classification
