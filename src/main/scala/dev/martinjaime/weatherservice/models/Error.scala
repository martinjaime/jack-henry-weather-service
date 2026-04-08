package dev.martinjaime.weatherservice.models

import io.circe.Codec
import sttp.tapir.Schema

object Error {
  case class ErrorResponse(error: String) derives Codec.AsObject, Schema
}
