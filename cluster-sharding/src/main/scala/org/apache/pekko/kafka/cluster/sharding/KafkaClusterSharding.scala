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

// license header from
// https://github.com/apache/kafka/blob/451fff8937c626a187371b8fcdf50cfb5177d38f/clients/src/main/java/org/apache/kafka/clients/producer/internals/DefaultPartitioner.java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.kafka.cluster.sharding

import java.util.concurrent.{ CompletionStage, ConcurrentHashMap }
import java.util.concurrent.atomic.AtomicInteger

import org.apache.pekko
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.scaladsl.adapter._
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId }
import pekko.annotation.{ ApiMayChange, InternalApi }
import pekko.cluster.sharding.external.ExternalShardAllocation
import pekko.cluster.sharding.typed.scaladsl.EntityTypeKey
import pekko.cluster.sharding.typed.{ ShardingEnvelope, ShardingMessageExtractor }
import pekko.cluster.typed.Cluster
import pekko.kafka.scaladsl.MetadataClient
import pekko.kafka._
import pekko.util.Timeout._
import org.apache.kafka.common.utils.Utils

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }
import pekko.util.FutureConverters._
import pekko.util.JavaDurationConverters._
import org.slf4j.LoggerFactory

/**
 * API MAY CHANGE
 *
 * Apache Pekko Extension to enable Apache Pekko Cluster External Sharding with Apache Pekko Connector Kafka.
 */
@ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
final class KafkaClusterSharding(system: ExtendedActorSystem) extends Extension {
  import KafkaClusterSharding._

  /**
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy will be automatically determined by querying the Kafka
   * cluster for the number of partitions of a user provided [[topic]]. Use the [[settings]] parameter to configure
   * the Kafka Consumer connection required to retrieve the number of partitions. Each call to this method will result
   * in a round trip to Kafka. This method should only be called once per entity type [[M]], per local actor system.
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractor[M](topic: String,
      timeout: FiniteDuration,
      settings: ConsumerSettings[_, _]): Future[KafkaShardingMessageExtractor[M]] =
    getPartitionCount(topic, timeout, settings).map(new KafkaShardingMessageExtractor[M](_))(system.dispatcher)

  /**
   * Java API
   *
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy will be automatically determined by querying the Kafka
   * cluster for the number of partitions of a user provided [[topic]]. Use the [[settings]] parameter to configure
   * the Kafka Consumer connection required to retrieve the number of partitions. Each call to this method will result
   * in a round trip to Kafka. This method should only be called once per entity type [[M]], per local actor system.
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractor[M](topic: String,
      timeout: java.time.Duration,
      settings: ConsumerSettings[_, _]): CompletionStage[KafkaShardingMessageExtractor[M]] =
    getPartitionCount(topic, timeout.asScala, settings)
      .map(new KafkaShardingMessageExtractor[M](_))(system.dispatcher)
      .asJava

  /**
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy is provided explicitly with [[kafkaPartitions]].
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractor[M](kafkaPartitions: Int): KafkaShardingMessageExtractor[M] =
    new KafkaShardingMessageExtractor[M](kafkaPartitions)

  /**
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy will be automatically determined by querying the Kafka
   * cluster for the number of partitions of a user provided [[topic]]. Use the [[settings]] parameter to configure
   * the Kafka Consumer connection required to retrieve the number of partitions. Use the [[entityIdExtractor]] to pick
   * a field from the Entity to use as the entity id for the hashing strategy. Each call to this method will result
   * in a round trip to Kafka. This method should only be called once per entity type [[M]], per local actor system.
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractorNoEnvelope[M](topic: String,
      timeout: FiniteDuration,
      entityIdExtractor: M => String,
      settings: ConsumerSettings[_, _]): Future[KafkaShardingNoEnvelopeExtractor[M]] =
    getPartitionCount(topic, timeout, settings)
      .map(partitions => new KafkaShardingNoEnvelopeExtractor[M](partitions, entityIdExtractor))(system.dispatcher)

  /**
   * Java API
   *
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy will be automatically determined by querying the Kafka
   * cluster for the number of partitions of a user provided [[topic]]. Use the [[settings]] parameter to configure
   * the Kafka Consumer connection required to retrieve the number of partitions. Use the [[entityIdExtractor]] to pick
   * a field from the Entity to use as the entity id for the hashing strategy. Each call to this method will result
   * in a round trip to Kafka. This method should only be called once per entity type [[M]], per local actor system.
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractorNoEnvelope[M](
      topic: String,
      timeout: java.time.Duration,
      entityIdExtractor: java.util.function.Function[M, String],
      settings: ConsumerSettings[_, _]): CompletionStage[KafkaShardingNoEnvelopeExtractor[M]] =
    getPartitionCount(topic, timeout.asScala, settings)
      .map(partitions => new KafkaShardingNoEnvelopeExtractor[M](partitions, e => entityIdExtractor.apply(e)))(
        system.dispatcher)
      .asJava

  /**
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy is provided explicitly with [[kafkaPartitions]].
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractorNoEnvelope[M](kafkaPartitions: Int,
      entityIdExtractor: M => String): KafkaShardingNoEnvelopeExtractor[M] =
    new KafkaShardingNoEnvelopeExtractor[M](kafkaPartitions, entityIdExtractor)

  /**
   * API MAY CHANGE
   *
   * Asynchronously return a [[pekko.cluster.sharding.typed.ShardingMessageExtractor]] with a default hashing strategy
   * based on Apache Kafka's [[org.apache.kafka.clients.producer.internals.DefaultPartitioner]].
   *
   * The number of partitions to use with the hashing strategy is provided explicitly with [[kafkaPartitions]].
   *
   * All topics used in a Consumer [[pekko.kafka.Subscription]] must contain the same number of partitions to ensure
   * that entities are routed to the same Entity type.
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def messageExtractorNoEnvelope[M](
      kafkaPartitions: Int,
      entityIdExtractor: java.util.function.Function[M, String]): KafkaShardingNoEnvelopeExtractor[M] =
    new KafkaShardingNoEnvelopeExtractor[M](kafkaPartitions, e => entityIdExtractor.apply(e))

  private val metadataConsumerActorNum = new AtomicInteger
  private def getPartitionCount[M](topic: String,
      timeout: FiniteDuration,
      settings: ConsumerSettings[_, _]): Future[Int] = {
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val num = metadataConsumerActorNum.getAndIncrement()
    val consumerActor = system
      .systemActorOf(KafkaConsumerActor.props(settings), s"metadata-consumer-actor-$num")
    val metadataClient = MetadataClient.create(consumerActor, timeout)
    val numPartitions = metadataClient.getPartitionsFor(topic).map(_.length)
    numPartitions.onComplete(_ => system.stop(consumerActor))
    numPartitions.map { count =>
      system.log.info("Retrieved {} partitions for topic '{}'", count, topic)
      count
    }
  }

  private val rebalanceListeners =
    new ConcurrentHashMap[EntityTypeKey[_], pekko.actor.typed.ActorRef[ConsumerRebalanceEvent]]()

  /**
   * API MAY CHANGE
   *
   * Create an Apache Pekko Connector Kafka rebalance listener that handles [[TopicPartitionsAssigned]] events. The [[typeKey]] is
   * used to create the [[ExternalShardAllocation]] client. When partitions are assigned to this consumer group member
   * the rebalance listener will use the [[ExternalShardAllocation]] client to update the External Sharding strategy
   * accordingly so that entities are (eventually) routed to the local Apache Pekko cluster member.
   *
   * Returns an Apache Pekko typed [[pekko.actor.typed.ActorRef]]. This must be converted to a classic actor before it can be
   * passed to an Apache Pekko Connector Kafka [[ConsumerSettings]].
   *
   * {{{
   * import org.apache.pekko
   * import pekko.actor.typed.scaladsl.adapter._
   * val listenerClassicActorRef: pekko.actor.ActorRef = listenerTypedActorRef.toClassic
   * }}}
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def rebalanceListener(typeKey: EntityTypeKey[_]): pekko.actor.typed.ActorRef[ConsumerRebalanceEvent] = {
    rebalanceListeners.computeIfAbsent(typeKey,
      _ => {
        system.toTyped
          .systemActorOf(RebalanceListener(typeKey), s"kafka-cluster-sharding-rebalance-listener-${typeKey.name}")
      })
  }

  /**
   * Java API
   *
   * API MAY CHANGE
   *
   * Create an Apache Pekko Connector Kafka rebalance listener that handles [[TopicPartitionsAssigned]] events. The [[typeKey]] is
   * used to create the [[ExternalShardAllocation]] client. When partitions are assigned to this consumer group member
   * the rebalance listener will use the [[ExternalShardAllocation]] client to update the External Sharding strategy
   * accordingly so that entities are (eventually) routed to the local Apache Pekko cluster member.
   *
   * Returns an Apache Pekko typed [[pekko.actor.typed.ActorRef]]. This must be converted to a classic actor before it can be
   * passed to an Apache Pekko Connector Kafka [[ConsumerSettings]].
   *
   * {{{
   * import org.apache.pekko
   * import pekko.actor.typed.scaladsl.adapter._
   * val listenerClassicActorRef: pekko.actor.ActorRef = listenerTypedActorRef.toClassic
   * }}}
   */
  @ApiMayChange(issue = "https://github.com/akka/alpakka-kafka/issues/1074")
  def rebalanceListener(
      typeKey: pekko.cluster.sharding.typed.javadsl.EntityTypeKey[_])
      : pekko.actor.typed.ActorRef[ConsumerRebalanceEvent] = {
    rebalanceListener(typeKey.asScala)
  }
}

object KafkaClusterSharding extends ExtensionId[KafkaClusterSharding] {
  @InternalApi
  sealed trait KafkaClusterShardingContract {
    def kafkaPartitions: Int
    def shardId(entityId: String): String = {
      // simplified version of Kafka's `DefaultPartitioner` implementation
      val partition = org.apache.kafka.common.utils.Utils
        .toPositive(Utils.murmur2(entityId.getBytes())) % kafkaPartitions
      partition.toString
    }
  }

  @InternalApi
  final class KafkaShardingMessageExtractor[M] private[kafka] (val kafkaPartitions: Int)
      extends ShardingMessageExtractor[ShardingEnvelope[M], M]
      with KafkaClusterShardingContract {
    override def entityId(envelope: ShardingEnvelope[M]): String = envelope.entityId
    override def unwrapMessage(envelope: ShardingEnvelope[M]): M = envelope.message
  }

  @InternalApi
  final class KafkaShardingNoEnvelopeExtractor[M] private[kafka] (val kafkaPartitions: Int,
      entityIdExtractor: M => String)
      extends ShardingMessageExtractor[M, M]
      with KafkaClusterShardingContract {
    override def entityId(message: M): String = entityIdExtractor(message)
    override def unwrapMessage(message: M): M = message
  }

  @InternalApi
  private[kafka] object RebalanceListener {

    // Used from future callbacks so can't use the log in the context
    private val log = LoggerFactory.getLogger(RebalanceListener.getClass)

    def apply(typeKey: EntityTypeKey[_]): Behavior[ConsumerRebalanceEvent] =
      Behaviors.setup { ctx =>
        import ctx.executionContext
        val shardAllocationClient = ExternalShardAllocation(ctx.system).clientFor(typeKey.name)
        val address = Cluster(ctx.system).selfMember.address
        Behaviors.receiveMessage[ConsumerRebalanceEvent] {
          case TopicPartitionsAssigned(_, partitions) =>
            if (log.isInfoEnabled) {
              log.info("Consumer group '{}' assigned topic partitions to cluster member '{}': [{}]",
                typeKey.name,
                address,
                partitions.mkString(","))
            }

            val updates = shardAllocationClient.updateShardLocations(partitions.map { tp =>
              val shardId = tp.partition().toString
              // the Kafka partition number becomes the Apache Pekko shard id
              (shardId, address)
            }.toMap)

            // There's no point blocking here because the rebalance listener is triggered asynchronously. If we want to block during
            // rebalance then we should provide an implementation using the `PartitionAssignmentHandler` instead
            updates
              .onComplete {
                case Success(_) =>
                  if (log.isInfoEnabled) {
                    log.info(
                      "Completed consumer group '{}' assignment of topic partitions to cluster member '{}': [{}]",
                      typeKey.name,
                      address,
                      partitions.mkString(","))
                  }

                case Failure(ex) =>
                  log.error("A failure occurred while updating cluster shards", ex)
              }
            Behaviors.same
          case TopicPartitionsRevoked(_, partitions) =>
            val partitionsList = partitions.mkString(",")
            log.info("Consumer group '{}' revoked topic partitions from cluster member '{}': [{}]",
              typeKey.name,
              address,
              partitionsList)
            Behaviors.same
        }
      }
  }

  override def createExtension(system: ExtendedActorSystem): KafkaClusterSharding =
    new KafkaClusterSharding(system)

  /**
   * Java API
   */
  override def get(system: ClassicActorSystemProvider): KafkaClusterSharding = super.get(system)

  /**
   * Java API
   */
  override def get(system: ActorSystem): KafkaClusterSharding = super.get(system)
}
