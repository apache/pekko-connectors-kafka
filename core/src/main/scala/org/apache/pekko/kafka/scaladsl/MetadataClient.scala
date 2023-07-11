/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.scaladsl

import java.util.concurrent.atomic.AtomicLong

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem, ExtendedActorSystem }
import pekko.dispatch.ExecutionContexts
import pekko.kafka.Metadata._
import pekko.kafka.{ ConsumerSettings, KafkaConsumerActor }
import pekko.pattern.ask
import pekko.util.Timeout
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{ PartitionInfo, TopicPartition }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class MetadataClient private (consumerActor: ActorRef, timeout: Timeout, managedActor: Boolean)(
    implicit ec: ExecutionContext) {

  def getBeginningOffsets(partitions: Set[TopicPartition]): Future[Map[TopicPartition, Long]] =
    (consumerActor ? GetBeginningOffsets(partitions))(timeout)
      .mapTo[BeginningOffsets]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  def getBeginningOffsetForPartition(partition: TopicPartition): Future[Long] =
    getBeginningOffsets(Set(partition))
      .map(beginningOffsets => beginningOffsets(partition))

  def getEndOffsets(partitions: Set[TopicPartition]): Future[Map[TopicPartition, Long]] =
    (consumerActor ? GetEndOffsets(partitions))(timeout)
      .mapTo[EndOffsets]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  def getEndOffsetForPartition(partition: TopicPartition): Future[Long] =
    getEndOffsets(Set(partition))
      .map(endOffsets => endOffsets(partition))

  def listTopics(): Future[Map[String, List[PartitionInfo]]] =
    (consumerActor ? ListTopics)(timeout)
      .mapTo[Topics]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  def getPartitionsFor(topic: String): Future[List[PartitionInfo]] =
    (consumerActor ? GetPartitionsFor(topic))(timeout)
      .mapTo[PartitionsFor]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  @deprecated("use `getCommittedOffsets`", "alpakka-kafka 2.0.3")
  def getCommittedOffset(partition: TopicPartition): Future[OffsetAndMetadata] =
    (consumerActor ? GetCommittedOffset(partition))(timeout)
      .mapTo[CommittedOffset]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  def getCommittedOffsets(partitions: Set[TopicPartition]): Future[Map[TopicPartition, OffsetAndMetadata]] =
    (consumerActor ? GetCommittedOffsets(partitions))(timeout)
      .mapTo[CommittedOffsets]
      .map(_.response)
      .flatMap {
        case Success(res) => Future.successful(res)
        case Failure(e)   => Future.failed(e)
      }(ExecutionContexts.parasitic)

  def close(): Unit =
    if (managedActor) {
      consumerActor ! KafkaConsumerActor.Stop
    }
}

object MetadataClient {
  private val actorCount = new AtomicLong()

  def create(consumerActor: ActorRef, timeout: Timeout)(implicit ec: ExecutionContext): MetadataClient =
    new MetadataClient(consumerActor, timeout, false)

  def create[K, V](
      consumerSettings: ConsumerSettings[K, V],
      timeout: Timeout)(implicit system: ActorSystem, ec: ExecutionContext): MetadataClient = {
    val consumerActor = system
      .asInstanceOf[ExtendedActorSystem]
      .systemActorOf(KafkaConsumerActor.props(consumerSettings),
        s"pekko-connectors-kafka-metadata-client-${actorCount.getAndIncrement()}")
    new MetadataClient(consumerActor, timeout, true)
  }
}
