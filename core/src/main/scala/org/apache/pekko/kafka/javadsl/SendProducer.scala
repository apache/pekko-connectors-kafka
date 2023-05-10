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

package org.apache.pekko.kafka.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.kafka.ProducerMessage._
import pekko.kafka.{ scaladsl, ProducerSettings }
import pekko.util.FutureConverters._
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }

/**
 * Utility class for producing to Kafka without using Apache Pekko Streams.
 */
final class SendProducer[K, V] private (underlying: scaladsl.SendProducer[K, V]) {

  // kept for bin-compatibility
  @deprecated("use the variant with ClassicActorSystemProvider instead", "2.0.5")
  private[kafka] def this(settings: ProducerSettings[K, V], system: ActorSystem) =
    this(scaladsl.SendProducer(settings)(system))

  /**
   * Utility class for producing to Kafka without using Apache Pekko Streams.
   * @param settings producer settings used to create or access the [[org.apache.kafka.clients.producer.Producer]]
   *
   * The internal asynchronous operations run on the provided `Executor` (which may be an `ActorSystem`'s dispatcher).
   */
  def this(settings: ProducerSettings[K, V], system: ClassicActorSystemProvider) =
    this(scaladsl.SendProducer(settings)(system.classicSystem))

  /**
   * Send records to Kafka topics and complete a future with the result.
   *
   * It publishes records to Kafka topics conditionally:
   *
   * - [[pekko.kafka.ProducerMessage.Message Message]] publishes a single message to its topic, and completes the future with [[pekko.kafka.ProducerMessage.Result Result]]
   *
   * - [[pekko.kafka.ProducerMessage.MultiMessage MultiMessage]] publishes all messages in its `records` field, and completes the future with [[pekko.kafka.ProducerMessage.MultiResult MultiResult]]
   *
   * - [[pekko.kafka.ProducerMessage.PassThroughMessage PassThroughMessage]] does not publish anything, and completes the future with [[pekko.kafka.ProducerMessage.PassThroughResult PassThroughResult]]
   *
   * The messages support passing through arbitrary data.
   */
  def sendEnvelope[PT](envelope: Envelope[K, V, PT]): CompletionStage[Results[K, V, PT]] =
    underlying.sendEnvelope(envelope).asJava

  /**
   * Send a raw Kafka [[org.apache.kafka.clients.producer.ProducerRecord]] and complete a future with the resulting metadata.
   */
  def send(record: ProducerRecord[K, V]): CompletionStage[RecordMetadata] =
    underlying.send(record).asJava

  /**
   * Close the underlying producer (depending on the "close producer on stop" setting).
   */
  def close(): CompletionStage[Done] = underlying.close().asJava

  override def toString: String = s"SendProducer(${underlying.settings})"
}
