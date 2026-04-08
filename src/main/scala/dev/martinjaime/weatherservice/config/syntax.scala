package dev.martinjaime.weatherservice.config

import cats.MonadThrow
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

// Might be overkill, but this is copied from an old project to also handle config
// errors on a generic config type
object syntax {
  extension (source: ConfigSource) {
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] = {
      F.pure(source.load[A]).flatMap {
        case Left(errors)  => F.raiseError[A](ConfigReaderException(errors))
        case Right(config) => F.pure(config)
      }
    }
  }
}
