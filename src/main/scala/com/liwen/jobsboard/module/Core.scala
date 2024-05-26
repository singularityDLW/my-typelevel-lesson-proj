package com.liwen.jobsboard.module

import com.liwen.jobsboard.algebras.*
import cats.effect.*
import cats.implicits.*
import doobie.util.transactor.Transactor

final case class Core[F[_]] private (val jobs: Jobs[F])

// posgres -> jobs -> core -> httpapi -> app
object Core {

  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}
