package com.liwen.jobsboard.algebras

import cats.*
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import com.liwen.jobsboard.domain.job.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.*

import java.util.UUID

// in charge of module business logic related to jobs
trait Jobs[F[_]] {
  // "algebras" the kind of operation, the kind of fundamental operations

  // algebras contains some operations
  // CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all: F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

/*
id: UUID,
date: Long,
ownerEmail: String,
jobInfo: JobInfo,
company: String,
title: String,
description: String,
externalUrl: String,
remote: Boolean,
location: String,
salaryLo: Option[Int],
salaryHi: Option[Int],
currency: Option[String],
country: Option[String],
tags: Option[List[String]],
image: Option[String],
seniority: Option[String],
other: Option[String],
active: Boolean = false

 */

class LiveJobs[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Jobs[F] {

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      insert into jobs(
        date,
        ownerEmail,
        jobInfo,
        active,
        company,
        title,
        description,
        externalUrl,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      ) values (
        ${System.currentTimeMillis()},
        ${ownerEmail},
        ${jobInfo.company},
        ${jobInfo.title},
        ${jobInfo.description},
        ${jobInfo.externalUrl},
        ${jobInfo.remote},
        ${jobInfo.location},
        ${jobInfo.salaryLo},
        ${jobInfo.salaryHi},
        ${jobInfo.currency},
        ${jobInfo.country},
        ${jobInfo.tags},
        ${jobInfo.image},
        ${jobInfo.seniority},
        ${jobInfo.other},
        false
      )
      """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all: F[List[Job]] =
    sql"""
      select
        id,
        date,
        ownerEmail,
        jobInfo,
        active,
        company,
        title,
        description,
        externalUrl,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      from jobs
    """
      .query[Job]
      .to[List]
      .transact(xa)

  override def find(id: UUID): F[Option[Job]] =
    sql"""
      select
        id,
        date,
        ownerEmail,
        jobInfo,
        active,
        company,
        title,
        description,
        externalUrl,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      from jobs
      where id=$id
    """.query[Job].option.transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs
      SET
        company = ${jobInfo.company}
        title = ${jobInfo.title}
        description = ${jobInfo.description}
        externalUrl = ${jobInfo.externalUrl}
        remote = ${jobInfo.remote}
        location = ${jobInfo.location}
        salaryLo = ${jobInfo.salaryLo}
        salaryHi = ${jobInfo.salaryHi}
        currency = ${jobInfo.currency}
        country = ${jobInfo.country}
        tags = ${jobInfo.tags}
        image = ${jobInfo.image}
        seniority = ${jobInfo.seniority}
        other = ${jobInfo.other}
      WHERE id = $id
    """.update.run
      .transact(xa)
      .flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
      DELETE from jobs
      WHERE id = $id
    """.update.run.transact(xa)
}

object LiveJobs {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
