package com.liwen.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.dsl.*
import org.http4s.*
import org.http4s.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.liwen.jobsboard.fixtures.JobFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.liwen.jobsboard.module.*
import com.liwen.jobsboard.algebras.*
import com.liwen.jobsboard.domain.job.*
import com.liwen.jobsboard.http.routes.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {
  // prep
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all: IO[List[Job]] =
      IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(AwesomeJob))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJob)
        IO.pure(1)
      else
        IO.pure(0)

  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val jobsRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  // tests
  "JobRoutes" - {
//    "should return a job with a given id" in {
//      // code under test
//      for {
//        // simulate an HTTP request
//        response <- jobsRoutes.orNotFound.run(
//          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
//        )
//        retrieved <- response.as[Job] // get http resp
//      } yield {
//        response.status shouldBe Status.Ok
//        retrieved shouldBe AwesomeJob
//      }
//    }

    "should return all jobs" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
        )
        retrieved <- response.as[List[Job]] // get http resp
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should create a new job" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        retrieved <- response.as[UUID] // get http resp
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe NewJobUuid
      }
    }

    "should update a job" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOk <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only update a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOk <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOk <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }
}
