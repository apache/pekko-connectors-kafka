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

package org.apache.pekko.kafka.internal

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorRef
import pekko.annotation.InternalApi
import pekko.kafka.scaladsl.Consumer.Control
import pekko.kafka.{ AutoSubscription, ConsumerSettings, ManualSubscription, Subscription }
import pekko.kafka.internal.SubSourceLogic._
import pekko.stream.SourceShape
import pekko.stream.scaladsl.Source
import pekko.stream.stage.{ AsyncCallback, GraphStageLogic }
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

/** Internal API */
@InternalApi
private[kafka] final class PlainSource[K, V](settings: ConsumerSettings[K, V], subscription: Subscription)
    extends KafkaSourceStage[K, V, ConsumerRecord[K, V]](s"PlainSource ${subscription.renderStageAttribute}") {
  override protected def logic(shape: SourceShape[ConsumerRecord[K, V]]): GraphStageLogic with Control =
    new SingleSourceLogic[K, V, ConsumerRecord[K, V]](shape, settings, subscription) with PlainMessageBuilder[K, V]
}

/** Internal API */
@InternalApi
private[kafka] final class ExternalPlainSource[K, V](consumer: ActorRef, subscription: ManualSubscription)
    extends KafkaSourceStage[K, V, ConsumerRecord[K, V]](
      s"ExternalPlainSubSource ${subscription.renderStageAttribute}") {
  override protected def logic(shape: SourceShape[ConsumerRecord[K, V]]): GraphStageLogic with Control =
    new ExternalSingleSourceLogic[K, V, ConsumerRecord[K, V]](shape, consumer, subscription)
      with PlainMessageBuilder[K, V]
      with MetricsControl
}

/**
 * INTERNAL API
 */
@InternalApi
private[kafka] final class PlainSubSource[K, V](
    settings: ConsumerSettings[K, V],
    subscription: AutoSubscription,
    getOffsetsOnAssign: Option[Set[TopicPartition] => Future[Map[TopicPartition, Long]]],
    onRevoke: Set[TopicPartition] => Unit)
    extends KafkaSourceStage[K, V, (TopicPartition, Source[ConsumerRecord[K, V], NotUsed])](
      s"PlainSubSource ${subscription.renderStageAttribute}") {
  override protected def logic(
      shape: SourceShape[(TopicPartition, Source[ConsumerRecord[K, V], NotUsed])]): GraphStageLogic with Control = {

    val factory = new SubSourceStageLogicFactory[K, V, ConsumerRecord[K, V]] {
      def create(
          shape: SourceShape[ConsumerRecord[K, V]],
          tp: TopicPartition,
          consumerActor: ActorRef,
          subSourceStartedCb: AsyncCallback[SubSourceStageLogicControl],
          subSourceCancelledCb: AsyncCallback[(TopicPartition, SubSourceCancellationStrategy)],
          actorNumber: Int): SubSourceStageLogic[K, V, ConsumerRecord[K, V]] =
        new SubSourceStageLogic[K, V, ConsumerRecord[K, V]](shape,
          tp,
          consumerActor,
          subSourceStartedCb,
          subSourceCancelledCb,
          actorNumber) with PlainMessageBuilder[K, V]
    }

    new SubSourceLogic[K, V, ConsumerRecord[K, V]](shape,
      settings,
      subscription,
      getOffsetsOnAssign,
      onRevoke,
      subSourceStageLogicFactory = factory)
  }
}
