val Http4sVersion          = "0.23.33"
val MunitVersion           = "1.1.1"
val MunitCatsEffectVersion = "2.1.0"
val tapirVersion           = "1.13.15"

Compile / run / fork := true

lazy val root = (project in file("."))
  .settings(
    organization := "dev.martinjaime",
    name         := "weather-service",
    version      := "0.0.1-SNAPSHOT",
    scalaVersion := "3.3.6",
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-ember-server"     % Http4sVersion,
      "org.http4s"                  %% "http4s-ember-client"     % Http4sVersion,
      "org.http4s"                  %% "http4s-circe"            % Http4sVersion,
      "org.http4s"                  %% "http4s-dsl"              % Http4sVersion,
      "org.scalameta"               %% "munit"                   % MunitVersion           % Test,
      "org.typelevel"               %% "munit-cats-effect"       % MunitCatsEffectVersion % Test,
      "ch.qos.logback"               % "logback-classic"         % "1.5.32"               % Runtime,
      "org.typelevel"               %% "log4cats-slf4j"          % "2.8.0",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.github.pureconfig"       %% "pureconfig-core"         % "0.17.10"
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x                   => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )
