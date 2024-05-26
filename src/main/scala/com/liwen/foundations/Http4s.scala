package com.liwen.foundations

import cats.{Applicative, Monad}
import cats.effect.{IO, IOApp}
import org.http4s.{Header, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.ember.server.EmberServerBuilder
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.headers.*
import cats.effect.*
import org.http4s.server.*
import org.typelevel.ci.CIString
import org.http4s.dsl.impl.*

import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate a http server with students and courses
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(
      id: String,
      title: String,
      year: Int,
      students: List[Student],
      instructorName: String
  )

  object CourseRepository {

    private val catsEffectCourse = Course(
      "13aeffce-1d48-41da-a36d-8a563808a5ad",
      "rock the jvm ultimate scala source",
      2022,
      List("liwen", "litong"),
      "martin odersky"
    )
    // a "database"
    private val courses: Map[String, Course] =
      Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCourseById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // GET localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  // GET locaohost:8080/courses/13aeffce-1d48-41da-a36d-8a563808a5ad/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher       extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(
            instructor
          ) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match
          case Some(year) =>
            year.fold(
              _ => BadRequest("Parameter 'year' is invalid"),
              year => Ok(courses.filter(_.year == year).asJson)
            )
          case None => Ok(courses.asJson)
        Ok()
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCourseById(courseId).map(_.students) match {
          case Some(students) =>
            Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "rock the jvm"))
          case None => NotFound(s"No course with $courseId was found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok("All going great!")
    }
  }

  // compose path or rather endpoints
  // its a nice way to split http logic in between several endpoint groups
  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  // combine group several group of endpoints under path prefix
  private def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routerWithPathPrefixes)
      .build
      .use(_ => IO.println("Server ready!") *> IO.never)
}
