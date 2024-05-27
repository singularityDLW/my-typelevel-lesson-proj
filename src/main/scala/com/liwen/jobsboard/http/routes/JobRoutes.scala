package com.liwen.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import cats.implicits.*
import com.liwen.jobsboard.domain.job.*
import com.liwen.jobsboard.http.responses.FailureResponse
import com.liwen.jobsboard.algebras.Jobs

import org.typelevel.log4cats.Logger

import scala.collection.mutable
import java.util.UUID
import cats.effect.*

import com.liwen.jobsboard.http.validation.syntax.*

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F] {

  // "database"
  private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } //TODO add query params and filters
  private val allJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobList <- jobs.all
      resp    <- Ok(jobList)
    } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job with $id not found."))
    }

  }

  // POST /jobs { jobInfo }
  import com.liwen.jobsboard.logging.syntax.*
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          jobInfo <- req
            .as[JobInfo]
            .logError(e => s"Parsing payload failed: $e") // parse JobInfo payload using Circe
          jobId <- jobs.create("TODO@exaple.com", jobInfo)
          resp  <- Created(jobId)
        } yield resp
      }
  }

  // PUT /jobs/uuid { jobDetails }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          jobInfo     <- req.as[JobInfo]
          maybeNewJob <- jobs.update(id, jobInfo)
          resp <- maybeNewJob match {
            case Some(job) => Ok()
            case None      => NotFound(FailureResponse(s"Cannot update job $id: not found"))
          }
        } yield resp
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      jobs.find(id) match {
        case Some(job) =>
          for {
            _    <- jobs.delete(id)
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

// tie job routes to the other health routes set of endpoints
// into one major set of APIs
object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
