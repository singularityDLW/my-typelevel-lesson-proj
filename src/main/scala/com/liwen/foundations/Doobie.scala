package com.liwen.foundations

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.util.ExecutionContexts

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO] (
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/demo",
    "docker",
    "docker",
    None
  )

  def findAllStudentsName: IO[List[String]] = {
    val query = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  def saveStudent(id: Int, name: String): IO[Int] =
    val query = sql"insert into students(id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)

  //read as case class with fragment
  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    //build sql statements out of fragment
    val selectFragment = fr"select id, name"
    val fromFragment = fr"from students"
    val whereFragment = fr"where left(name, 1) = $letter"

    val statement = selectFragment ++ fromFragment ++ whereFragment
    val action = statement.query[Student].to[List]
    action.transact(xa)
  }

  // organize code
  // repository
  trait Students[F[_]] {
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      override def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id=$id".query[Student].option.transact(xa)

      override def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)

      override def create(name: String): F[Int] =
        sql"insert into students(name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  }

  val postgresResource = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/demo",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val smallProgram = postgresResource.use {xa =>
    val studentsRepo = Students[IO](xa)
    for {
      id <- studentsRepo.create("hanqing")
      hanqing <- studentsRepo.findById(id)
      _ <- IO.println(s"our baby is $hanqing")
    } yield ()
  }

  override def run: IO[Unit] = smallProgram
}
