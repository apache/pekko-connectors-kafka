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

package org.apache.pekko.kafka.internal

import org.apache.pekko
import pekko.actor.{ ActorRef, Status, Terminated }
import pekko.annotation.InternalApi
import pekko.kafka.Subscriptions.{ Assignment, AssignmentOffsetsForTimes, AssignmentWithOffset }
import pekko.kafka.{ ConsumerFailed, ManualSubscription }
import pekko.stream.SourceShape
import pekko.stream.stage.GraphStageLogic.StageActor
import pekko.stream.stage.{ AsyncCallback, GraphStageLogic, OutHandler }
import org.apache.kafka.common.TopicPartition

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Internal API.
 *
 * Shared GraphStageLogic for [[SingleSourceLogic]] and [[ExternalSingleSourceLogic]].
 */
@InternalApi private abstract class BaseSingleSourceLogic[K, V, Msg](
    val shape: SourceShape[Msg]) extends GraphStageLogic(shape)
    with PromiseControl
    with MetricsControl
    with StageIdLogging
    with SourceLogicSubscription
    with MessageBuilder[K, V, Msg]
    with SourceLogicBuffer[K, V, Msg] {

  override protected def executionContext: ExecutionContext = materializer.executionContext
  protected def consumerFuture: Future[ActorRef]
  protected final var consumerActor: ActorRef = _
  protected var sourceActor: StageActor = _
  protected var tps = Set.empty[TopicPartition]
  private var requested = false
  private var requestId = 0

  private val assignedCB: AsyncCallback[Set[TopicPartition]] = getAsyncCallback[Set[TopicPartition]] { assignedTps =>
    tps ++= assignedTps
    log.debug("Assigned partitions: {}. All partitions: {}", assignedTps, tps)
    requestMessages()
  }

  private val revokedCB: AsyncCallback[Set[TopicPartition]] = getAsyncCallback[Set[TopicPartition]] { revokedTps =>
    tps --= revokedTps
    log.debug("Revoked partitions: {}. All partitions: {}", revokedTps, tps)
  }

  override def preStart(): Unit = {
    super.preStart()

    sourceActor = getStageActor(messageHandling)
    log.info("Starting. StageActor {}", sourceActor.ref)
    consumerActor = createConsumerActor()
    sourceActor.watch(consumerActor)

    configureSubscription(assignedCB, revokedCB)
  }

  protected def messageHandling: PartialFunction[(ActorRef, Any), Unit] = {
    case (_, msg: KafkaConsumerActor.Internal.Messages[K @unchecked, V @unchecked]) =>
      // might be more than one in flight when we assign/revoke tps
      if (msg.requestId == requestId)
        requested = false
      buffer = buffer ++ msg.messages
      pump()
    case (_, Status.Failure(e)) =>
      failStage(e)
    case (_, Terminated(ref)) if ref == consumerActor =>
      failStage(new ConsumerFailed())
  }

  protected def createConsumerActor(): ActorRef

  override protected def configureManualSubscription(subscription: ManualSubscription): Unit = subscription match {
    case Assignment(topics) =>
      consumerActor.tell(KafkaConsumerActor.Internal.Assign(topics), sourceActor.ref)
      tps ++= topics
    case AssignmentWithOffset(topics) =>
      consumerActor.tell(KafkaConsumerActor.Internal.AssignWithOffset(topics), sourceActor.ref)
      tps ++= topics.keySet
    case AssignmentOffsetsForTimes(topics) =>
      consumerActor.tell(KafkaConsumerActor.Internal.AssignOffsetsForTimes(topics), sourceActor.ref)
      tps ++= topics.keySet
  }

  @tailrec
  private def pump(): Unit =
    if (isAvailable(shape.out)) {
      if (buffer.hasNext) {
        val msg = buffer.next()
        push(shape.out, createMessage(msg))
        pump()
      } else if (!requested && tps.nonEmpty) {
        requestMessages()
      }
    }

  protected def requestMessages(): Unit = {
    requested = true
    requestId += 1
    log.debug("Requesting messages, requestId: {}, partitions: {}", requestId, tps)
    consumerActor.tell(KafkaConsumerActor.Internal.RequestMessages(requestId, tps), sourceActor.ref)
  }

  setHandler(shape.out,
    new OutHandler {
      override def onPull(): Unit = pump()
      override def onDownstreamFinish(cause: Throwable): Unit =
        performShutdown()
    })

  override def postStop(): Unit = {
    onShutdown()
    super.postStop()
  }

  def performShutdown(): Unit =
    log.info("Completing")
}
