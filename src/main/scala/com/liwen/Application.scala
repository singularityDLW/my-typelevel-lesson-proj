package com.liwen

import cats.effect.*
import cats.implicits.*
import com.liwen.jobsboard.config.*
import com.liwen.jobsboard.config.syntax.*
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.Logger
import cats.implicits.*
import com.liwen.jobsboard.module.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  // by the time build this EmberServer builder, already have the config ready setup
  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig) =>
      val app = for {
        xa      <- Database.makePostgresResource[IO](postgresConfig)
        core    <- Core[IO](xa)
        httpApi <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server
      app.use(_ => IO.println("Server ready!") *> IO.never)
  }
}
