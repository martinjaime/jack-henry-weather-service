package dev.martinjaime.weatherservice.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

case class AppConfig(port: Port, host: Host, weather: AppConfig.WeatherConfig) derives ConfigReader
object AppConfig {
  case class WeatherConfig(feelsLikeHotThresholdF: Double) derives ConfigReader

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
