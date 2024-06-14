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

import org.apache.pekko
import pekko.annotation.{ ApiMayChange, InternalApi }
import pekko.kafka.ConsumerMessage.{ PartitionOffset, TransactionalMessage }
import pekko.kafka.ProducerMessage._
import pekko.kafka.internal.{
  TransactionalProducerStage,
  TransactionalSource,
  TransactionalSourceWithOffsetContext,
  TransactionalSubSource
}
import pekko.kafka.scaladsl.Consumer.Control
import pekko.kafka.{ AutoSubscription, ConsumerMessage, ConsumerSettings, ProducerSettings, Subscription }
import pekko.stream.ActorAttributes
import pekko.stream.scaladsl.{ Flow, FlowWithContext, Keep, Sink, Source, SourceWithContext }
import pekko.{ Done, NotUsed }
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

/**
 * Apache Pekko Stream connector to support transactions between Kafka topics.
 */
object Transactional {

  /**
   * Transactional source to setup a stream for Exactly Only Once (EoS) kafka message semantics.  To enable EoS it's
   * necessary to use the [[Transactional.sink]] or [[Transactional.flow]] (for passthrough).
   */
  def source[K, V](settings: ConsumerSettings[K, V],
      subscription: Subscription): Source[TransactionalMessage[K, V], Control] =
    Source.fromGraph(new TransactionalSource[K, V](settings, subscription))

  /**
   * API MAY CHANGE
   *
   * This source is intended to be used with Apache Pekko's [flow with context](https://pekko.apache.org/docs/pekko/current/stream/operators/Flow/asFlowWithContext.html)
   * and [[Transactional.flowWithOffsetContext]].
   */
  @ApiMayChange
  def sourceWithOffsetContext[K, V](
      settings: ConsumerSettings[K, V],
      subscription: Subscription): SourceWithContext[ConsumerRecord[K, V], PartitionOffset, Control] =
    Source
      .fromGraph(new TransactionalSourceWithOffsetContext[K, V](settings, subscription))
      .asSourceWithContext(_._2)
      .map(_._1)

  /**
   * Internal API. Work in progress.
   *
   * The `partitionedSource` is a way to track automatic partition assignment from kafka.
   * Each source is setup for for Exactly Only Once (EoS) kafka message semantics.
   * To enable EoS it's necessary to use the [[Transactional.sink]] or [[Transactional.flow]] (for passthrough).
   * When Kafka rebalances partitions, all sources complete before the remaining sources are issued again.
   *
   * By generating the `transactionalId` from the [[TopicPartition]], multiple instances of your application can run
   * without having to manually assign partitions to each instance.
   */
  @ApiMayChange
  @InternalApi
  private[kafka] def partitionedSource[K, V](
      settings: ConsumerSettings[K, V],
      subscription: AutoSubscription): Source[(TopicPartition, Source[TransactionalMessage[K, V], NotUsed]), Control] =
    Source.fromGraph(new TransactionalSubSource[K, V](settings, subscription))

  /**
   * Sink that is aware of the [[ConsumerMessage.TransactionalMessage.partitionOffset]] from a [[Transactional.source]].  It will
   * initialize, begin, produce, and commit the consumer offset as part of a transaction.
   */
  def sink[K, V](
      settings: ProducerSettings[K, V],
      transactionalId: String): Sink[Envelope[K, V, ConsumerMessage.PartitionOffset], Future[Done]] =
    flow(settings, transactionalId).toMat(Sink.ignore)(Keep.right)

  /**
   * API MAY CHANGE
   *
   * Sink that requires the context to be [[ConsumerMessage.PartitionOffset]] from a [[Transactional.sourceWithOffsetContext]].
   * It will initialize, begin, produce, and commit the consumer offset as part of a transaction.
   */
  @ApiMayChange
  def sinkWithOffsetContext[K, V](
      settings: ProducerSettings[K, V],
      transactionalId: String): Sink[(Envelope[K, V, NotUsed], PartitionOffset), Future[Done]] =
    sink(settings, transactionalId)
      .contramap {
        case (env, offset) =>
          env.withPassThrough(offset)
      }

  /**
   * Publish records to Kafka topics and then continue the flow. The flow can only be used with a [[Transactional.source]] that
   * emits a [[ConsumerMessage.TransactionalMessage]]. The flow requires a unique `transactional.id` across all app
   * instances.  The flow will override producer properties to enable Kafka exactly-once transactional support.
   */
  def flow[K, V](
      settings: ProducerSettings[K, V],
      transactionalId: String): Flow[Envelope[K, V, ConsumerMessage.PartitionOffset],
    Results[K, V,
      ConsumerMessage.PartitionOffset], NotUsed] = {
    require(transactionalId != null && transactionalId.length > 0, "You must define a Transactional id.")
    require(settings.producerFactorySync.isEmpty, "You cannot use a shared or external producer factory.")

    val flow = Flow
      .fromGraph(
        new TransactionalProducerStage[K, V, ConsumerMessage.PartitionOffset](settings, transactionalId))
      .mapAsync(settings.parallelism)(identity)

    flowWithDispatcher(settings, flow)
  }

  /**
   * API MAY CHANGE
   *
   * Publish records to Kafka topics and then continue the flow. The flow can only be used with a [[Transactional.sourceWithOffsetContext]] which
   * carries [[ConsumerMessage.PartitionOffset]] as context. The flow requires a unique `transactional.id` across all app
   * instances. The flow will override producer properties to enable Kafka exactly-once transactional support.
   *
   * This flow is intended to be used with Apache Pekko's [flow with context](https://pekko.apache.org/docs/pekko/current/stream/operators/Flow/asFlowWithContext.html)
   * and [[Transactional.sourceWithOffsetContext]].
   */
  @ApiMayChange
  def flowWithOffsetContext[K, V](
      settings: ProducerSettings[K, V],
      transactionalId: String): FlowWithContext[Envelope[K, V, NotUsed],
    ConsumerMessage.PartitionOffset, Results[K, V, ConsumerMessage.PartitionOffset],
    ConsumerMessage.PartitionOffset, NotUsed] = {
    val noContext: Flow[Envelope[K, V, PartitionOffset], Results[K, V, PartitionOffset], NotUsed] =
      flow(settings, transactionalId)
    noContext
      .asFlowWithContext[Envelope[K, V, NotUsed], PartitionOffset, PartitionOffset] {
        case (env, c) => env.withPassThrough(c)
      }(res => res.passThrough)
  }

  private def flowWithDispatcher[PassThrough, V, K](
      settings: ProducerSettings[K, V],
      flow: Flow[Envelope[K, V, PassThrough], Results[K, V, PassThrough], NotUsed]) =
    if (settings.dispatcher.isEmpty) flow
    else flow.withAttributes(ActorAttributes.dispatcher(settings.dispatcher))
}
