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

object WeatherServiceRoutesTest extends SimpleIOSuite {

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val weatherConfig = WeatherConfig(feelsLikeHotThresholdF = 75.0, feelsLikeColdThresholdF = 60.0)

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

  private val basicReq = Request[IO](
    method = Method.GET,
    uri = uri"/current-forecast".withQueryParam("lat", 0.0).withQueryParam("lon", 0.0)
  )

  private val hotFResponse = NatGridPointsPeriods(
    temperature = weatherConfig.feelsLikeHotThresholdF.toInt + 5,
    temperatureUnit = "F",
    shortForecast = "Sunny"
  )

  private val coldFResponse = NatGridPointsPeriods(
    temperature = weatherConfig.feelsLikeColdThresholdF.toInt - 5,
    temperatureUnit = "F",
    shortForecast = "Cloudy"
  )

  test("current-forecast returns hot weather") {
    for {
      res  <- appReturning(hotFResponse).run(basicReq)
      body <- res.as[WeatherSummary]
    } yield expect(res.status == Status.Ok) &&
      expect(body.condition == "Sunny") &&
      expect(body.feelsLike == "hot")
  }

  test("current-forecast returns cold weather") {
    for {
      res  <- appReturning(coldFResponse).run(basicReq)
      body <- res.as[WeatherSummary]
    } yield expect(res.status == Status.Ok) &&
      expect(body.condition == "Cloudy") &&
      expect(body.feelsLike == "cold")
  }

  test("current-forecast returns moderate weather") {
    val moderateRes = hotFResponse.copy(temperature = weatherConfig.feelsLikeColdThresholdF.toInt + 5)
    for {
      res  <- appReturning(moderateRes).run(basicReq)
      body <- res.as[WeatherSummary]
    } yield expect(res.status == Status.Ok) &&
      expect(body.condition == "Sunny") &&
      expect(body.feelsLike == "moderate")
  }

  test("current-forecast returns summary and converts a cold C to F") {
    for {
      res <- appReturning(NatGridPointsPeriods(temperature = 25, temperatureUnit = "C", shortForecast = "Sunny"))
        .run(basicReq)
      body <- res.as[WeatherSummary]
    } yield expect(res.status == Status.Ok) &&
      expect(body.condition == "Sunny") &&
      expect(body.feelsLike == "hot")
  }

  test("current-forecast returns an error response for unsupported temperature unit") {
    for {
      res <- appReturning(NatGridPointsPeriods(temperature = 80, temperatureUnit = "K", shortForecast = "Clear"))
        .run(basicReq)
      body <- res.as[ErrorResponse]
    } yield expect(res.status == Status.BadRequest) &&
      expect(body.error.contains("Unsupported temperature unit"))
  }

  test("current-forecast returns 500 for unexpected service errors") {
    for {
      res  <- appFailingWith(new RuntimeException("boom")).run(basicReq)
      body <- res.as[ErrorResponse]
    } yield expect(res.status == Status.InternalServerError) &&
      expect(body.error == "Internal server error")
  }
}
