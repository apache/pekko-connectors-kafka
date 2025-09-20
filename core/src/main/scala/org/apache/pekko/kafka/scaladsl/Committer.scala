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
import pekko.annotation.ApiMayChange
import pekko.kafka.CommitterSettings
import pekko.kafka.ConsumerMessage.{ Committable, CommittableOffsetBatch }
import pekko.kafka.internal.CommitCollectorStage
import pekko.stream.scaladsl.{ Flow, FlowWithContext, Keep, Sink }
import pekko.{ Done, NotUsed }

import scala.concurrent.{ ExecutionContext, Future }

object Committer {

  /**
   * Batches offsets and commits them to Kafka, emits `Done` for every committed batch.
   */
  def flow(settings: CommitterSettings): Flow[Committable, Done, NotUsed] =
    batchFlow(settings).map(_ => Done)

  /**
   * Batches offsets and commits them to Kafka, emits `CommittableOffsetBatch` for every committed batch.
   */
  def batchFlow(settings: CommitterSettings): Flow[Committable, CommittableOffsetBatch, NotUsed] = {
    val offsetBatches: Flow[Committable, CommittableOffsetBatch, NotUsed] =
      Flow
        .fromGraph(new CommitCollectorStage(settings))

    // See https://github.com/akka/alpakka-kafka/issues/882
    import pekko.kafka.CommitDelivery._
    settings.delivery match {
      case WaitForAck =>
        offsetBatches
          .mapAsyncUnordered(settings.parallelism) { batch =>
            batch.commitInternal().map(_ => batch)(ExecutionContext.parasitic)
          }
      case SendAndForget =>
        offsetBatches.map(_.tellCommit())
    }
  }

  /**
   * API MAY CHANGE
   *
   * Batches offsets from context and commits them to Kafka, emits no useful value, but keeps the committed
   * `CommittableOffsetBatch` as context.
   */
  @ApiMayChange
  def flowWithOffsetContext[E](
      settings: CommitterSettings): FlowWithContext[E, Committable, NotUsed, CommittableOffsetBatch, NotUsed] = {
    val value = Flow[(E, Committable)]
      .map(_._2)
      .via(batchFlow(settings))
      .map(b => (NotUsed, b))
    new FlowWithContext(value)
  }

  /**
   * Batches offsets and commits them to Kafka.
   */
  def sink(settings: CommitterSettings): Sink[Committable, Future[Done]] =
    flow(settings)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * API MAY CHANGE
   *
   * Batches offsets from context and commits them to Kafka.
   */
  @ApiMayChange
  def sinkWithOffsetContext[E](settings: CommitterSettings): Sink[(E, Committable), Future[Done]] =
    Flow[(E, Committable)]
      .via(flowWithOffsetContext(settings))
      .toMat(Sink.ignore)(Keep.right)

}
