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
import pekko.actor.{ ActorRef, ExtendedActorSystem, Terminated }
import pekko.annotation.InternalApi
import pekko.kafka.internal.KafkaConsumerActor.Internal.Messages
import pekko.kafka.scaladsl.PartitionAssignmentHandler
import pekko.kafka.{ ConsumerSettings, RestrictedConsumer, Subscription }
import pekko.stream.SourceShape
import org.apache.kafka.common.TopicPartition

import scala.concurrent.{ Future, Promise }

/**
 * Internal API.
 *
 * Anonymous sub-class instances are created in [[CommittableSource]] and [[TransactionalSource]].
 */
@InternalApi private abstract class SingleSourceLogic[K, V, Msg](
    shape: SourceShape[Msg],
    settings: ConsumerSettings[K, V],
    override protected val subscription: Subscription) extends BaseSingleSourceLogic[K, V, Msg](shape) {

  override protected def logSource: Class[_] = classOf[SingleSourceLogic[K, V, Msg]]
  private val consumerPromise = Promise[ActorRef]()
  final val actorNumber = KafkaConsumerActor.Internal.nextNumber()

  final def consumerFuture: Future[ActorRef] = consumerPromise.future

  final def createConsumerActor(): ActorRef = {
    val extendedActorSystem = materializer.system.asInstanceOf[ExtendedActorSystem]
    val actor =
      extendedActorSystem.systemActorOf(pekko.kafka.KafkaConsumerActor.props(sourceActor.ref, settings),
        s"kafka-consumer-$actorNumber")
    consumerPromise.success(actor)
    actor
  }

  final override def postStop(): Unit = {
    consumerActor.tell(KafkaConsumerActor.Internal.StopFromStage(id), sourceActor.ref)
    super.postStop()
  }

  final override def performShutdown(): Unit = {
    super.performShutdown()
    setKeepGoing(true)
    if (!isClosed(shape.out)) {
      complete(shape.out)
    }
    sourceActor.become(shuttingDownReceive.orElse {
      case (_, Messages(requestId, messages)) =>
        // Prevent stage failure during shutdown by ignoring Messages
        if (messages.hasNext)
          log.debug("Unexpected `Messages` received with requestId={} and a non-empty message iterator: {}",
            requestId,
            messages.mkString(", "))
    })
    stopConsumerActor()
  }

  protected def shuttingDownReceive: PartialFunction[(ActorRef, Any), Unit] = {
    case (_, Terminated(ref)) if ref == consumerActor =>
      onShutdown()
      completeStage()
  }

  protected def stopConsumerActor(): Unit =
    materializer.scheduleOnce(settings.stopTimeout,
      new Runnable {
        override def run(): Unit =
          consumerActor.tell(KafkaConsumerActor.Internal.StopFromStage(id), sourceActor.ref)
      })

  /**
   * Opportunity for subclasses to add a different logic to the partition assignment callbacks.
   */
  override protected def addToPartitionAssignmentHandler(
      handler: PartitionAssignmentHandler): PartitionAssignmentHandler = {
    val flushMessagesOfRevokedPartitions: PartitionAssignmentHandler = new PartitionAssignmentHandler {
      private var lastRevoked = Set.empty[TopicPartition]

      override def onRevoke(revokedTps: Set[TopicPartition], consumer: RestrictedConsumer): Unit =
        lastRevoked = revokedTps

      override def onAssign(assignedTps: Set[TopicPartition], consumer: RestrictedConsumer): Unit =
        filterRevokedPartitionsCB.invoke(lastRevoked -- assignedTps)

      override def onLost(lostTps: Set[TopicPartition], consumer: RestrictedConsumer): Unit =
        filterRevokedPartitionsCB.invoke(lostTps)

      override def onStop(revokedTps: Set[TopicPartition], consumer: RestrictedConsumer): Unit = ()
    }
    new PartitionAssignmentHelpers.Chain(handler, flushMessagesOfRevokedPartitions)
  }
}
