package dev.martinjaime.weatherservice

import cats.effect.IO
import dev.martinjaime.weatherservice.config.AppConfig.WeatherConfig
import dev.martinjaime.weatherservice.models.Error.ErrorResponse
import dev.martinjaime.weatherservice.models.WeatherModels.{NatGridPointsPeriods, WeatherSummary}
import org.http4s.{Method, Request, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import weaver.SimpleIOSuite

object WeatherServiceRoutesWeaverSuite extends SimpleIOSuite {

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val weatherConfig = WeatherConfig(feelsLikeHotThresholdF = 70.0)

  private def appFailingWith(error: Throwable) = {
    val service = new NationalWeatherService[IO] {
      override def getForecast(lat: Double, lon: Double): IO[NatGridPointsPeriods] = IO.raiseError(error)
    }

    WeatherServiceRoutes.routes[IO](service, weatherConfig).orNotFound
  }

  private def appReturning(period: NatGridPointsPeriods) = {
    val service = new NationalWeatherService[IO] {
      override def getForecast(lat: Double, lon: Double): IO[NatGridPointsPeriods] = IO.pure(period)
    }

    WeatherServiceRoutes.routes[IO](service, weatherConfig).orNotFound
  }

  test("current-forecast returns summary and converts C to F") {
    val req = Request[IO](
      method = Method.GET,
      uri = uri"/current-forecast".withQueryParam("lat", 37.0).withQueryParam("lon", -94.5)
    )

    for {
      res <- appReturning(NatGridPointsPeriods(temperature = 25, temperatureUnit = "C", shortForecast = "Sunny")).run(req)
      body <- res.as[WeatherSummary]
    } yield expect(res.status == Status.Ok) &&
      expect(body.condition == "Sunny") &&
      expect(body.feelsLike == "hot")
  }

  test("current-forecast returns an error response for unsupported temperature unit") {
    val req = Request[IO](
      method = Method.GET,
      uri = uri"/current-forecast".withQueryParam("lat", 35.2).withQueryParam("lon", -97.4)
    )

    for {
      res <- appReturning(NatGridPointsPeriods(temperature = 80, temperatureUnit = "K", shortForecast = "Clear")).run(req)
      body <- res.as[ErrorResponse]
    } yield expect(res.status == Status.BadRequest) &&
      expect(body.error.contains("Unsupported temperature unit"))
  }

  test("current-forecast returns 500 for unexpected service errors") {
    val req = Request[IO](
      method = Method.GET,
      uri = uri"/current-forecast".withQueryParam("lat", 40.7).withQueryParam("lon", -74.0)
    )

    for {
      res <- appFailingWith(new RuntimeException("boom")).run(req)
      body <- res.as[ErrorResponse]
    } yield expect(res.status == Status.InternalServerError) &&
      expect(body.error == "Internal server error")
  }
}

