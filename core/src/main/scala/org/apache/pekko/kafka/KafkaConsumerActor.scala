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

package org.apache.pekko.kafka

import org.apache.pekko
import pekko.actor.{ ActorRef, NoSerializationVerificationNeeded, Props }
import pekko.annotation.InternalApi
import pekko.kafka.internal.{ KafkaConsumerActor => InternalKafkaConsumerActor }

object KafkaConsumerActor {

  @InternalApi
  private[kafka] trait StopLike

  /**
   * Message to send for stopping the Kafka consumer actor.
   */
  case object Stop extends NoSerializationVerificationNeeded with StopLike

  /**
   * Java API:
   * Message to send for stopping the Kafka consumer actor.
   */
  val stop = Stop

  case class StoppingException() extends RuntimeException("Kafka consumer is stopping")

  /**
   * Creates Props for the Kafka Consumer Actor.
   */
  def props[K, V](settings: ConsumerSettings[K, V]): Props =
    Props(new InternalKafkaConsumerActor(None, settings)).withDispatcher(settings.dispatcher)

  /**
   * Creates Props for the Kafka Consumer Actor with a reference back to the owner of it
   * which will be signalled with [[pekko.actor.Status.Failure Failure(exception)]], in case the
   * Kafka client instance can't be created.
   */
  def props[K, V](owner: ActorRef, settings: ConsumerSettings[K, V]): Props =
    Props(new InternalKafkaConsumerActor(Some(owner), settings)).withDispatcher(settings.dispatcher)
}
