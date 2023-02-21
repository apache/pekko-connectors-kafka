/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.testkit

import org.apache.pekko.Done
import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.kafka.ConsumerMessage
import org.apache.pekko.kafka.ConsumerMessage.{ CommittableOffset, GroupTopicPartition, PartitionOffsetCommittedMarker }
import org.apache.pekko.kafka.internal.{ CommittableOffsetImpl, KafkaAsyncConsumerCommitterRef }
import org.apache.kafka.clients.consumer.{ ConsumerRecord, OffsetAndMetadata }
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

/**
 * Factory methods to create instances that normally are emitted by [[org.apache.pekko.kafka.scaladsl.Consumer]] and [[org.apache.pekko.kafka.javadsl.Consumer]] flows.
 */
@ApiMayChange
object ConsumerResultFactory {

  val fakeCommitter: KafkaAsyncConsumerCommitterRef = new KafkaAsyncConsumerCommitterRef(null, null)(
    ec = scala.concurrent.ExecutionContext.global) {
    private val done = Future.successful(Done)

    override def commitSingle(topicPartition: TopicPartition, offset: OffsetAndMetadata): Future[Done] = done

    override def commitOneOfMulti(topicPartition: TopicPartition, offset: OffsetAndMetadata): Future[Done] = done

    override def tellCommit(topicPartition: TopicPartition, offset: OffsetAndMetadata, emergency: Boolean): Unit = ()
  }

  def partitionOffset(groupId: String, topic: String, partition: Int, offset: Long): ConsumerMessage.PartitionOffset =
    new ConsumerMessage.PartitionOffset(ConsumerMessage.GroupTopicPartition(groupId, topic, partition), offset)

  def partitionOffset(key: GroupTopicPartition, offset: Long) = ConsumerMessage.PartitionOffset(key, offset)

  def committableOffset(groupId: String,
      topic: String,
      partition: Int,
      offset: Long,
      metadata: String): ConsumerMessage.CommittableOffset =
    committableOffset(partitionOffset(groupId, topic, partition, offset), metadata)

  def committableOffset(partitionOffset: ConsumerMessage.PartitionOffset,
      metadata: String): ConsumerMessage.CommittableOffset =
    CommittableOffsetImpl(partitionOffset, metadata)(fakeCommitter)

  def committableMessage[K, V](
      record: ConsumerRecord[K, V],
      committableOffset: CommittableOffset): ConsumerMessage.CommittableMessage[K, V] =
    ConsumerMessage.CommittableMessage(record, committableOffset)

  def transactionalMessage[K, V](
      record: ConsumerRecord[K, V],
      partitionOffset: PartitionOffsetCommittedMarker): ConsumerMessage.TransactionalMessage[K, V] =
    ConsumerMessage.TransactionalMessage(record, partitionOffset)

}
