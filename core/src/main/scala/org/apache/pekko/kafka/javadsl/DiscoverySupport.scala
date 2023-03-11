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
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.kafka.{ scaladsl, ConsumerSettings, ProducerSettings }
import com.typesafe.config.Config

import scala.compat.java8.FunctionConverters._
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

/**
 * Scala API.
 *
 * Reads Kafka bootstrap servers from configured sources via [[org.apache.pekko.discovery.Discovery]] configuration.
 */
object DiscoverySupport {

  /**
   * Expects a `service` section in the given Config and reads the given service name's address
   * to be used as `bootstrapServers`.
   */
  def consumerBootstrapServers[K, V](
      config: Config,
      system: ClassicActorSystemProvider)
      : java.util.function.Function[ConsumerSettings[K, V], CompletionStage[ConsumerSettings[K, V]]] = {
    implicit val sys: ClassicActorSystemProvider = system
    val function: ConsumerSettings[K, V] => Future[ConsumerSettings[K, V]] =
      scaladsl.DiscoverySupport.consumerBootstrapServers(config)
    function.andThen(FutureConverters.toJava).asJava
  }

  // kept for bin-compatibility
  def consumerBootstrapServers[K, V](
      config: Config,
      system: ActorSystem)
      : java.util.function.Function[ConsumerSettings[K, V], CompletionStage[ConsumerSettings[K, V]]] = {
    val sys: ClassicActorSystemProvider = system
    consumerBootstrapServers(config, sys)
  }

  /**
   * Expects a `service` section in the given Config and reads the given service name's address
   * to be used as `bootstrapServers`.
   */
  def producerBootstrapServers[K, V](
      config: Config,
      system: ClassicActorSystemProvider)
      : java.util.function.Function[ProducerSettings[K, V], CompletionStage[ProducerSettings[K, V]]] = {
    implicit val sys: ClassicActorSystemProvider = system
    val function: ProducerSettings[K, V] => Future[ProducerSettings[K, V]] =
      scaladsl.DiscoverySupport.producerBootstrapServers(config)
    function.andThen(FutureConverters.toJava).asJava
  }

  // kept for bin-compatibility
  def producerBootstrapServers[K, V](
      config: Config,
      system: ActorSystem)
      : java.util.function.Function[ProducerSettings[K, V], CompletionStage[ProducerSettings[K, V]]] = {
    val sys: ClassicActorSystemProvider = system
    producerBootstrapServers(config, sys)
  }
}
