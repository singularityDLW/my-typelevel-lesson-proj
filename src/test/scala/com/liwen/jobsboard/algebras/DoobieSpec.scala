package com.liwen.jobsboard.algebras

import cats.effect.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.implicits.*
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec {
  // simulate a database
  // docker containers
  // testContainers
  
  // to be implemented by whatever test case interacts with the DB
  val initScript: String

  // simulate a database
  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container = new PostgreSQLContainer("postgres").withInitScript(initScript)
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())

    Resource.make(acquire)(release)
  }

  // setup a postgres transactor
  val transactor: Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ec <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      db.getJdbcUrl,
      db.getUsername,
      db.getPassword,
      ec
    )
  } yield xa
}
