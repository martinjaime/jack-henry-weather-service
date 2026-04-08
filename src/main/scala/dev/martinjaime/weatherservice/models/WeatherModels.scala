package dev.martinjaime.weatherservice.models

import io.circe.Codec
import sttp.tapir.Schema

object WeatherModels {
  case class WeatherSummary(
    condition: String,
    feelsLike: String
  ) derives Codec.AsObject,
        Schema

  case class NatPointsResProperties(
    forecast: String
  ) derives Codec.AsObject

  case class NatPointsRes(
    properties: NatPointsResProperties
  ) derives Codec.AsObject

  case class NatGridPointsPeriods(
    temperature: Int,
    temperatureUnit: String,
    shortForecast: String
  ) derives Codec.AsObject

  case class NatGridPointsResProperties(
    periods: List[NatGridPointsPeriods]
  ) derives Codec.AsObject

  case class NatGridPointsRes(
    properties: NatGridPointsResProperties
  ) derives Codec.AsObject
}
