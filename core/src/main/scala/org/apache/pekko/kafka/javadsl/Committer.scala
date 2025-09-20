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

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.japi.Pair
import pekko.{ Done, NotUsed }
import pekko.kafka.ConsumerMessage.{ Committable, CommittableOffsetBatch }
import pekko.kafka.{ scaladsl, CommitterSettings }
import pekko.stream.javadsl.{ Flow, FlowWithContext, Sink }
import scala.jdk.FutureConverters._

object Committer {

  /**
   * Batches offsets and commits them to Kafka, emits `Done` for every committed batch.
   */
  def flow[C <: Committable](settings: CommitterSettings): Flow[C, Done, NotUsed] =
    scaladsl.Committer.flow(settings).asJava

  /**
   * Batches offsets and commits them to Kafka, emits `CommittableOffsetBatch` for every committed batch.
   */
  def batchFlow[C <: Committable](settings: CommitterSettings): Flow[C, CommittableOffsetBatch, NotUsed] =
    scaladsl.Committer.batchFlow(settings).asJava

  /**
   * API MAY CHANGE
   *
   * Batches offsets from context and commits them to Kafka, emits no useful value, but keeps the committed
   * `CommittableOffsetBatch` as context.
   */
  @ApiMayChange
  def flowWithOffsetContext[E, C <: Committable](
      settings: CommitterSettings): FlowWithContext[E, C, NotUsed, CommittableOffsetBatch, NotUsed] =
    scaladsl.Committer.flowWithOffsetContext[E](settings).asJava

  /**
   * Batches offsets and commits them to Kafka.
   */
  def sink[C <: Committable](settings: CommitterSettings): Sink[C, CompletionStage[Done]] =
    scaladsl.Committer.sink(settings).mapMaterializedValue(_.asJava).asJava

  /**
   * API MAY CHANGE
   *
   * Batches offsets from context and commits them to Kafka.
   */
  @ApiMayChange
  def sinkWithOffsetContext[E, C <: Committable](
      settings: CommitterSettings): Sink[Pair[E, C], CompletionStage[Done]] =
    pekko.stream.scaladsl
      .Flow[Pair[E, C]]
      .map(_.toScala)
      .toMat(scaladsl.Committer.sinkWithOffsetContext(settings))(pekko.stream.scaladsl.Keep.right)
      .mapMaterializedValue[CompletionStage[Done]](_.asJava)
      .asJava[Pair[E, C]]
}
