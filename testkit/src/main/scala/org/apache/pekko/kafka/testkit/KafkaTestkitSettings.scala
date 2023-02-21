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

package org.apache.pekko.kafka.testkit

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration._

class KafkaTestkitSettings private (val clusterTimeout: FiniteDuration,
    val consumerGroupTimeout: FiniteDuration,
    val checkInterval: FiniteDuration) {

  /**
   * Java Api
   */
  def getClusterTimeout(): java.time.Duration = java.time.Duration.ofMillis(clusterTimeout.toMillis)

  /**
   * Java Api
   */
  def getConsumerGroupTimeout(): java.time.Duration = java.time.Duration.ofMillis(consumerGroupTimeout.toMillis)

  /**
   * Java Api
   */
  def getCheckInterval(): java.time.Duration = java.time.Duration.ofMillis(checkInterval.toMillis)
}

object KafkaTestkitSettings {
  final val ConfigPath = "pekko.kafka.testkit"

  /**
   * Create testkit settings from ActorSystem settings.
   */
  def apply(system: ActorSystem): KafkaTestkitSettings =
    KafkaTestkitSettings(system.settings.config.getConfig(ConfigPath))

  /**
   * Java Api
   *
   * Create testkit settings from ActorSystem settings.
   */
  def create(system: ActorSystem): KafkaTestkitSettings = KafkaTestkitSettings(system)

  /**
   * Create testkit settings from a Config.
   */
  def apply(config: Config): KafkaTestkitSettings = {
    val clusterTimeout = config.getDuration("cluster-timeout").toMillis.millis
    val consumerGroupTimeout = config.getDuration("consumer-group-timeout").toMillis.millis
    val checkInterval = config.getDuration("check-interval").toMillis.millis

    new KafkaTestkitSettings(clusterTimeout, consumerGroupTimeout, checkInterval)
  }

  /**
   * Java Api
   *
   * Create testkit settings from a Config.
   */
  def create(config: Config): KafkaTestkitSettings = KafkaTestkitSettings(config)
}
