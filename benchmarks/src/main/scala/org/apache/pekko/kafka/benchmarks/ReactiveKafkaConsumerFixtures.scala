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

package org.apache.pekko.kafka.benchmarks

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.kafka.ConsumerMessage.CommittableMessage
import pekko.kafka.benchmarks.app.RunTestCommand
import pekko.kafka.scaladsl.Consumer
import pekko.kafka.scaladsl.Consumer.Control
import pekko.kafka.{ ConsumerSettings, Subscriptions }
import pekko.stream.scaladsl.Source
import org.apache.kafka.clients.consumer.{ ConsumerConfig, ConsumerRecord }
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }

case class ReactiveKafkaConsumerTestFixture[T](topic: String,
    msgCount: Int,
    source: Source[T, Control],
    numberOfPartitions: Int)

object ReactiveKafkaConsumerFixtures extends PerfFixtureHelpers {

  private def createConsumerSettings(kafkaHost: String)(implicit actorSystem: ActorSystem) =
    ConsumerSettings(actorSystem, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaHost)
      .withGroupId(randomId())
      .withClientId(randomId())
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def plainSources(c: RunTestCommand)(implicit actorSystem: ActorSystem) =
    FixtureGen[ReactiveKafkaConsumerTestFixture[ConsumerRecord[Array[Byte], String]]](
      c,
      msgCount => {
        fillTopic(c.filledTopic, c.kafkaHost)
        val settings = createConsumerSettings(c.kafkaHost)
        val source = Consumer.plainSource(settings, Subscriptions.topics(c.filledTopic.topic))
        ReactiveKafkaConsumerTestFixture(c.filledTopic.topic, msgCount, source, c.numberOfPartitions)
      })

  def committableSources(c: RunTestCommand)(implicit actorSystem: ActorSystem) =
    FixtureGen[ReactiveKafkaConsumerTestFixture[CommittableMessage[Array[Byte], String]]](
      c,
      msgCount => {
        fillTopic(c.filledTopic, c.kafkaHost)
        val settings = createConsumerSettings(c.kafkaHost)
        val source = Consumer.committableSource(settings, Subscriptions.topics(c.filledTopic.topic))
        ReactiveKafkaConsumerTestFixture(c.filledTopic.topic, msgCount, source, c.numberOfPartitions)
      })

  def noopFixtureGen(c: RunTestCommand) =
    FixtureGen[ReactiveKafkaConsumerTestFixture[ConsumerRecord[Array[Byte], String]]](
      c,
      msgCount => {
        ReactiveKafkaConsumerTestFixture("topic", msgCount, null, c.numberOfPartitions)
      })

}
