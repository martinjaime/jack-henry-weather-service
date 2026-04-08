package dev.martinjaime.weatherservice

import cats.effect.*
import cats.syntax.all.*
import dev.martinjaime.weatherservice.config.AppConfig.WeatherConfig
import dev.martinjaime.weatherservice.models.Error.ErrorResponse
import dev.martinjaime.weatherservice.models.WeatherModels.WeatherSummary
import org.http4s.HttpRoutes
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter

object WeatherServiceRoutes {

  val currentForecast: PublicEndpoint[(Double, Double), ErrorResponse, WeatherSummary, Any] =
    endpoint.get
      .in("current-forecast")
      .in(query[Double]("lat"))
      .in(query[Double]("lon"))
      .out(jsonBody[WeatherSummary])
      .errorOut(jsonBody[ErrorResponse])
      .description("Get current weather summary by lat/lon")

  // Converts known units to Fahrenheit, then classifies into a simple hot/cold label.
  private def parseFeelsLike(temp: Int, unit: String, hotThresholdF: Double): Either[Throwable, String] = {
    val fahrenheitEither = unit.trim.toUpperCase match {
      case "F"         => Right(temp.toDouble)
      case "C"         => Right((temp * 9.0 / 5.0) + 32.0)
      case unsupported => Left(new IllegalArgumentException(s"Unsupported temperature unit: $unsupported"))
    }

    fahrenheitEither.map { fahrenheit =>
      if (fahrenheit >= hotThresholdF) "hot" else "cold"
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
          feelsLike <- Async[F].fromEither(
            parseFeelsLike(res.temperature, res.temperatureUnit, weatherConfig.feelsLikeHotThresholdF)
          )
          summary = WeatherSummary(
            feelsLike = feelsLike,
            condition = res.shortForecast
          )
        } yield summary

        result.attempt.map {
          case Right(summary) => Right(summary)
          case Left(error)    => Left(ErrorResponse(error.getMessage))
        }
      }
    )
  }
}
