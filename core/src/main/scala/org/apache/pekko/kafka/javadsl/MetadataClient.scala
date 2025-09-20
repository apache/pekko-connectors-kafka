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

package org.apache.pekko.kafka.javadsl

import java.util.concurrent.{ CompletionStage, Executor }

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem }
import pekko.kafka.ConsumerSettings
import pekko.util.Timeout
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{ PartitionInfo, TopicPartition }

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

class MetadataClient private (metadataClient: pekko.kafka.scaladsl.MetadataClient) {

  def getBeginningOffsets[K, V](
      partitions: java.util.Set[TopicPartition]): CompletionStage[java.util.Map[TopicPartition, java.lang.Long]] =
    metadataClient
      .getBeginningOffsets(partitions.asScala.toSet)
      .map { beginningOffsets =>
        beginningOffsets.view.mapValues(Long.box).toMap.asJava
      }(ExecutionContext.parasitic)
      .asJava

  def getBeginningOffsetForPartition[K, V](partition: TopicPartition): CompletionStage[java.lang.Long] =
    metadataClient
      .getBeginningOffsetForPartition(partition)
      .map(Long.box)(ExecutionContext.parasitic)
      .asJava

  def getEndOffsets(
      partitions: java.util.Set[TopicPartition]): CompletionStage[java.util.Map[TopicPartition, java.lang.Long]] =
    metadataClient
      .getEndOffsets(partitions.asScala.toSet)
      .map { endOffsets =>
        endOffsets.view.mapValues(Long.box).toMap.asJava
      }(ExecutionContext.parasitic)
      .asJava

  def getEndOffsetForPartition(partition: TopicPartition): CompletionStage[java.lang.Long] =
    metadataClient
      .getEndOffsetForPartition(partition)
      .map(Long.box)(ExecutionContext.parasitic)
      .asJava

  def listTopics(): CompletionStage[java.util.Map[java.lang.String, java.util.List[PartitionInfo]]] =
    metadataClient
      .listTopics()
      .map { topics =>
        topics.view.mapValues(partitionsInfo => partitionsInfo.asJava).toMap.asJava
      }(ExecutionContext.parasitic)
      .asJava

  def getPartitionsFor(topic: java.lang.String): CompletionStage[java.util.List[PartitionInfo]] =
    metadataClient
      .getPartitionsFor(topic)
      .map { partitionsInfo =>
        partitionsInfo.asJava
      }(ExecutionContext.parasitic)
      .asJava

  @deprecated("use `getCommittedOffsets`", "Alpakka Kafka 2.0.3")
  def getCommittedOffset(partition: TopicPartition): CompletionStage[OffsetAndMetadata] =
    metadataClient
      .getCommittedOffset(partition)
      .asJava

  def getCommittedOffsets(
      partitions: java.util.Set[TopicPartition]): CompletionStage[java.util.Map[TopicPartition, OffsetAndMetadata]] =
    metadataClient
      .getCommittedOffsets(partitions.asScala.toSet)
      .map { committedOffsets =>
        committedOffsets.asJava
      }(ExecutionContext.parasitic)
      .asJava

  def close(): Unit =
    metadataClient.close()
}

object MetadataClient {

  def create(consumerActor: ActorRef, timeout: Timeout, executor: Executor): MetadataClient = {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
    val metadataClient = pekko.kafka.scaladsl.MetadataClient.create(consumerActor, timeout)
    new MetadataClient(metadataClient)
  }

  def create[K, V](consumerSettings: ConsumerSettings[K, V],
      timeout: Timeout,
      system: ActorSystem,
      executor: Executor): MetadataClient = {
    val metadataClient = pekko.kafka.scaladsl.MetadataClient
      .create(consumerSettings, timeout)(system, ExecutionContext.fromExecutor(executor))
    new MetadataClient(metadataClient)
  }
}
