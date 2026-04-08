package dev.martinjaime.weatherservice

import dev.martinjaime.weatherservice.models.WeatherModels.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.circe.*
import org.http4s.Method.*
import org.typelevel.log4cats.LoggerFactory

trait NationalWeatherService[F[_]] {
  def getForecast(lat: Double, lon: Double): F[NatGridPointsPeriods]
}

object NationalWeatherService {
  def apply[F[_]](using ev: NationalWeatherService[F]): NationalWeatherService[F] = ev

  def impl[F[_]: Concurrent: LoggerFactory](C: Client[F]): NationalWeatherService[F] = new NationalWeatherService[F]:
    private val logger = LoggerFactory[F].getLogger

    val dsl = new Http4sClientDsl[F] {}
    import dsl.*

    def getPoints(lat: Double, lon: Double): F[NatPointsRes] = {
      val path = uri"https://api.weather.gov/points" / s"$lat,$lon"
      C.expect[NatPointsRes](GET(path))
        .handleErrorWith { error =>
          logger.error(error)("Could not fetch upstream points response") *>
            Concurrent[F].raiseError(new Throwable("Could not fetch weather data"))
        }
    }

    def getGridPoints(forecastUrl: String): F[NatGridPointsRes] = {
      // Ideally, the path is built from the response rather than blindly using the url we get back,
      // but for simplicity, we'll just use the url directly.
      // val path = uri"https://api.weather.gov/gridpoints" / gridId / s"$gridX,$gridY" / "forecast"
      val path = Uri.unsafeFromString(forecastUrl)
      C.expect[NatGridPointsRes](GET(path))
        .handleErrorWith { error =>
          logger.error(error)("Could not fetch upstream grid points response") *>
            Concurrent[F].raiseError(new Throwable("Could not fetch weather data"))
        }
    }

    def getForecast(lat: Double, lon: Double): F[NatGridPointsPeriods] = {
      for {
        pointsRes <- getPoints(lat, lon)
        gridRes   <- getGridPoints(pointsRes.properties.forecast)
        res <- gridRes.properties.periods.headOption match {
          case Some(period) => Concurrent[F].pure(period)
          case None =>
            logger.error("No periods found in response") *>
              Concurrent[F].raiseError(new Throwable("No forecast found"))
        }
      } yield res
    }
}
