package com.liwen.jobsboard.algebras

import cats.effect.*
import cats.effect.implicits.*

import cats.effect.testing.scalatest.AsyncIOSpec
import com.liwen.jobsboard.fixtures.JobFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import doobie.implicits.*
import doobie.util.*
import doobie.*
import doobie.postgres.implicits.*

class JobsSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with JobFixture {
  val initScript: String = "sql/jobs.sql"

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(NotFoundJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should retrieve a job by id" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(AwesomeJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))
      }
    }

    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.findAll
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }

    "should create a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("liwen@test.com", RockTheJvmNewJob)
          maybeJob <- jobs.find(jobId)
        } yield maybeJob

        program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
      }
    }

    "should return an updated job if exists" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob

        program.asserting(_ shouldBe Some(UpdatedAwesomeJob))
      }
    }

    "should return None when update an not-existed job" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob

        program.asserting(_ shouldBe None)
      }
    }

    "should delete an existed job" in {
      transactor.use { xa =>
        val program = for {
          jobs             <- LiveJobs[IO](xa)
          numOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          countOfJobs <- sql"select count(*) from jobs where id = $AwesomeJobUuid"
            .query[Int]
            .unique
            .transact(xa)
        } yield (numOfDeletedJobs, countOfJobs)

        program.asserting { case (numOfDeletedJobs, countOfJobs) =>
          numOfDeletedJobs shouldBe 1
          countOfJobs shouldBe 0
        }
      }
    }

    "should return zero updated rows if job ID to delete is not found" in {
      transactor.use { xa =>
        val program = for {
          jobs             <- LiveJobs[IO](xa)
          numOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
        } yield numOfDeletedJobs

        program.asserting(_ shouldBe 0)
      }
    }
  }
}
