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

package org.apache.pekko.kafka.benchmarks

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.kafka.ConsumerMessage.TransactionalMessage
import pekko.kafka.ProducerMessage.{ Envelope, Results }
import pekko.kafka.benchmarks.app.RunTestCommand
import pekko.kafka.scaladsl.Consumer.Control
import pekko.kafka.scaladsl.Transactional
import pekko.kafka.{ ConsumerMessage, ConsumerSettings, ProducerSettings, Subscriptions }
import pekko.stream.scaladsl.{ Flow, Source }
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  ByteArraySerializer,
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.duration.FiniteDuration

case class ReactiveKafkaTransactionTestFixture[SOut, FIn, FOut](sourceTopic: String,
    sinkTopic: String,
    msgCount: Int,
    source: Source[SOut, Control],
    flow: Flow[FIn, FOut, NotUsed])

object ReactiveKafkaTransactionFixtures extends PerfFixtureHelpers {
  type Key = Array[Byte]
  type Val = String
  type PassThrough = ConsumerMessage.PartitionOffset
  type KTransactionMessage = TransactionalMessage[Key, Val]
  type KProducerMessage = Envelope[Key, Val, PassThrough]
  type KResult = Results[Key, Val, PassThrough]

  private def createConsumerSettings(kafkaHost: String)(implicit actorSystem: ActorSystem) =
    ConsumerSettings(actorSystem, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaHost)
      .withGroupId(randomId())
      .withClientId(randomId())
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private def createProducerSettings(
      kafkaHost: String)(implicit actorSystem: ActorSystem): ProducerSettings[Array[Byte], String] =
    ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(kafkaHost)

  def transactionalSourceAndSink(c: RunTestCommand, commitInterval: FiniteDuration)(implicit actorSystem: ActorSystem) =
    FixtureGen[ReactiveKafkaTransactionTestFixture[KTransactionMessage, KProducerMessage, KResult]](
      c,
      msgCount => {
        fillTopic(c.filledTopic, c.kafkaHost)
        val sinkTopic = randomId()

        val consumerSettings = createConsumerSettings(c.kafkaHost)
        val source: Source[KTransactionMessage, Control] =
          Transactional.source(consumerSettings, Subscriptions.topics(c.filledTopic.topic))

        val producerSettings = createProducerSettings(c.kafkaHost).withEosCommitInterval(commitInterval)
        val flow: Flow[KProducerMessage, KResult, NotUsed] = Transactional.flow(producerSettings, randomId())

        ReactiveKafkaTransactionTestFixture[KTransactionMessage, KProducerMessage, KResult](c.filledTopic.topic,
          sinkTopic,
          msgCount,
          source,
          flow)
      })

  def noopFixtureGen(c: RunTestCommand) =
    FixtureGen[ReactiveKafkaTransactionTestFixture[KTransactionMessage, KProducerMessage, KResult]](c,
      msgCount => {
        ReactiveKafkaTransactionTestFixture("sourceTopic", "sinkTopic", msgCount, source = null, flow = null)
      })
}
