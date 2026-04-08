package dev.martinjaime.weatherservice

import cats.effect.*
import cats.syntax.all.*
import dev.martinjaime.weatherservice.config.AppConfig.WeatherConfig
import dev.martinjaime.weatherservice.models.Error.Error
import dev.martinjaime.weatherservice.models.Error.ErrorResponse
import dev.martinjaime.weatherservice.models.WeatherModels.WeatherSummary
import org.http4s.HttpRoutes
import org.typelevel.log4cats.LoggerFactory
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter

object WeatherServiceRoutes {

  val currentForecast: PublicEndpoint[(Double, Double), (StatusCode, ErrorResponse), WeatherSummary, Any] =
    endpoint.get
      .in("current-forecast")
      .in(query[Double]("lat"))
      .in(query[Double]("lon"))
      .out(jsonBody[WeatherSummary])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .description("Get current weather summary by lat/lon")

  // Converts known units to Fahrenheit, then classifies into a simple hot/cold/moderate label.
  private def parseFeelsLike(temp: Int, unit: String, weatherConfig: WeatherConfig): Either[Error, String] = {
    val fahrenheitEither = unit.trim.toUpperCase match {
      case "F"         => Right(temp.toDouble)
      case "C"         => Right((temp * 9.0 / 5.0) + 32.0)
      case unsupported => Left(Error(s"Unsupported temperature unit: $unsupported"))
    }

    fahrenheitEither.map { fahrenheit =>
      // An enum for hot/cold/moderate would be better
      if (fahrenheit > weatherConfig.feelsLikeHotThresholdF) "hot"
      else if (fahrenheit < weatherConfig.feelsLikeColdThresholdF) "cold"
      else "moderate"
    }
  }

  def routes[F[_]: Async: LoggerFactory](
    weatherService: NationalWeatherService[F],
    weatherConfig: WeatherConfig
  ): HttpRoutes[F] = {
    val logger = LoggerFactory[F].getLogger

    Http4sServerInterpreter[F]().toRoutes(
      currentForecast.serverLogic { case (lat, lon) =>
        // I would normally extract the following into its own WeatherService
        val result = for {
          _   <- logger.info(s"Received request for current forecast at lat: $lat, lon: $lon")
          res <- weatherService.getForecast(lat, lon)
        } yield parseFeelsLike(res.temperature, res.temperatureUnit, weatherConfig).map { feelsLike =>
          WeatherSummary(
            feelsLike = feelsLike,
            condition = res.shortForecast
          )
        }

        result.attempt.flatMap {
          case Right(Right(summary)) => Async[F].pure(Right(summary))
          case Right(Left(error)) =>
            logger
              .warn(s"Bad request while parsing feels-like value: ${error.message}")
              .as(Left((StatusCode.BadRequest, ErrorResponse(error.message))))
          case Left(error) =>
            logger
              .error(error)(s"Failed to get current forecast for lat: $lat, lon: $lon")
              .as(Left((StatusCode.InternalServerError, ErrorResponse("Internal server error"))))
        }
      }
    )
  }
}
