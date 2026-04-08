package dev.martinjaime.weatherservice.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

case class AppConfig(port: Port, host: Host, weather: AppConfig.WeatherConfig) derives ConfigReader
object AppConfig {
  case class WeatherConfig(
    feelsLikeHotThresholdF: Double,
    feelsLikeColdThresholdF: Double
  ) derives ConfigReader

  // reader that checks that hot temp is over cold temp
  given weatherConfigReader: ConfigReader[WeatherConfig] = ConfigReader
    .forProduct2(
      "feelsLikeHotThresholdF",
      "feelsLikeColdThresholdF"
    )(WeatherConfig.apply)
    .emap { config =>
      if (config.feelsLikeHotThresholdF > config.feelsLikeColdThresholdF) Right(config)
      else
        Left(
          CannotConvert(
            s"hot threshold ${config.feelsLikeHotThresholdF} must be greater than cold threshold ${config.feelsLikeColdThresholdF}",
            WeatherConfig.getClass.toString,
            "Invalid weather configuration"
          )
        )
    }

  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostStr =>
    Host
      .fromString(hostStr)
      .toRight(
        CannotConvert(hostStr, Host.getClass.toString, s"Invalid host value: $hostStr")
      )
  }

  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(
        CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port value: $portInt")
      )
  }
}
