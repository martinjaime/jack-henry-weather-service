package dev.martinjaime.weatherservice

import cats.effect.Async
import cats.syntax.all.*
import dev.martinjaime.weatherservice.config.AppConfig
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import dev.martinjaime.weatherservice.config.syntax.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object WeatherServiceServer:

  def run[F[_]: Async: Network]: F[Nothing] = ConfigSource.default.loadF[F, AppConfig].flatMap { config =>
    given LoggerFactory[F] = Slf4jFactory.create[F]

    val server = for {
      client <- EmberClientBuilder.default[F].build
      weatherService = NationalWeatherService.impl[F](client)
      swaggerRoutes = Http4sServerInterpreter[F]().toRoutes(
        SwaggerInterpreter().fromEndpoints(
          List(WeatherServiceRoutes.currentForecast),
          "Weather Service API",
          "1.0"
        )
      )
      httpApp      = (WeatherServiceRoutes.routes[F](weatherService, config.weather) <+> swaggerRoutes).orNotFound
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <- EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(finalHttpApp)
        .build
    } yield ()

    server.useForever
  }
