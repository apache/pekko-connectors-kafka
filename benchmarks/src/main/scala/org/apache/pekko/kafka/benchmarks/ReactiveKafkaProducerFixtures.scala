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
import pekko.kafka.ProducerMessage.{ Envelope, Results }
import pekko.kafka.ProducerSettings
import pekko.kafka.benchmarks.app.RunTestCommand
import pekko.kafka.scaladsl.Producer
import pekko.stream.scaladsl.Flow
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }

object ReactiveKafkaProducerFixtures extends PerfFixtureHelpers {

  val Parallelism = 10000

  type K = Array[Byte]
  type V = String
  type In[PassThrough] = Envelope[K, V, PassThrough]
  type Out[PassThrough] = Results[K, V, PassThrough]
  type FlowType[PassThrough] = Flow[In[PassThrough], Out[PassThrough], NotUsed]

  case class ReactiveKafkaProducerTestFixture[PassThrough](topic: String,
      msgCount: Int,
      msgSize: Int,
      flow: FlowType[PassThrough],
      numberOfPartitions: Int)

  private def createProducerSettings(kafkaHost: String)(implicit actorSystem: ActorSystem): ProducerSettings[K, V] =
    ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(kafkaHost)
      .withParallelism(Parallelism)

  def flowFixture(c: RunTestCommand)(implicit actorSystem: ActorSystem) =
    FixtureGen[ReactiveKafkaProducerTestFixture[Int]](
      c,
      msgCount => {
        val flow: FlowType[Int] = Producer.flexiFlow(createProducerSettings(c.kafkaHost))
        fillTopic(c.filledTopic.copy(msgCount = 1), c.kafkaHost)
        ReactiveKafkaProducerTestFixture(c.filledTopic.topic, msgCount, c.msgSize, flow, c.numberOfPartitions)
      })

  def noopFixtureGen(c: RunTestCommand) =
    FixtureGen[ReactiveKafkaConsumerTestFixture[ConsumerRecord[Array[Byte], String]]](
      c,
      msgCount => {
        ReactiveKafkaConsumerTestFixture("topic", msgCount, null, c.numberOfPartitions)
      })

}
