package com.liwen.jobsboard.http.validation

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.liwen.jobsboard.http.responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import validators.*
import org.typelevel.log4cats.Logger
import com.liwen.jobsboard.logging.syntax.*
import com.liwen.jobsboard.http.responses.*
import org.http4s.dsl.io.BadRequest

object syntax {

  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension (req: Request[F])
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(using
          EntityDecoder[F, A]
      ): F[Response[F]] =
        req
          .as[A]
          .logError(e => "Parsing payload failed: $e")
          .map(validateEntity)
          .flatMap {
            case Valid(entity) =>
              serverLogicIfValid(entity) // F[Response[F]]
            case Invalid(error) =>
              BadRequest(FailureResponse(error.toList.map(_.errorMessage).mkString(",")))
          }
  }

}
