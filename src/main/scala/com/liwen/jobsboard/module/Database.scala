package com.liwen.jobsboard.module

import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import cats.effect.*
import com.liwen.jobsboard.config.*

object Database {
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url, // todo to move to config
        config.user,
        config.password,
        ec
      )
    } yield xa
}
