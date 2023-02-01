/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.internal
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.kafka.{ AutoSubscription, ManualSubscription, Subscription }
import org.apache.pekko.kafka.Subscriptions._
import org.apache.pekko.kafka.scaladsl.PartitionAssignmentHandler
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage.{ AsyncCallback, GraphStageLogic }
import org.apache.kafka.common.TopicPartition

/**
 * Common subscription logic that's shared across sources.
 *
 * The implementation can inject its own behaviour in two ways:
 *
 * 1. Asynchronously by providing [[AsyncCallback]]s for rebalance events
 * 2. Synchronously by overriding `addToPartitionAssignmentHandler`
 */
@InternalApi
private[kafka] trait SourceLogicSubscription {
  self: GraphStageLogic =>

  protected def subscription: Subscription

  protected def consumerActor: ActorRef
  protected def sourceActor: StageActor

  protected def configureSubscription(partitionAssignedCB: AsyncCallback[Set[TopicPartition]],
      partitionRevokedCB: AsyncCallback[Set[TopicPartition]]): Unit = {

    def rebalanceListener(autoSubscription: AutoSubscription): PartitionAssignmentHandler = {
      PartitionAssignmentHelpers.chain(
        addToPartitionAssignmentHandler(autoSubscription.partitionAssignmentHandler),
        new PartitionAssignmentHelpers.AsyncCallbacks(autoSubscription,
          sourceActor.ref,
          partitionAssignedCB,
          partitionRevokedCB))
    }

    subscription match {
      case sub @ TopicSubscription(topics, _, _) =>
        consumerActor.tell(
          KafkaConsumerActor.Internal.Subscribe(
            topics,
            addToPartitionAssignmentHandler(rebalanceListener(sub))),
          sourceActor.ref)
      case sub @ TopicSubscriptionPattern(topics, _, _) =>
        consumerActor.tell(
          KafkaConsumerActor.Internal.SubscribePattern(
            topics,
            addToPartitionAssignmentHandler(rebalanceListener(sub))),
          sourceActor.ref)
      case s: ManualSubscription => configureManualSubscription(s)
    }
  }

  protected def configureManualSubscription(subscription: ManualSubscription): Unit = ()

  /**
   * Opportunity for subclasses to add a different logic to the partition assignment callbacks.
   */
  protected def addToPartitionAssignmentHandler(handler: PartitionAssignmentHandler): PartitionAssignmentHandler =
    handler
}
