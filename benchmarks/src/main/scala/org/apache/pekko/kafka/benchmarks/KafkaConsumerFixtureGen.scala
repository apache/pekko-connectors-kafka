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
import pekko.kafka.benchmarks.app.RunTestCommand
import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }

import scala.jdk.CollectionConverters._

case class KafkaConsumerTestFixture(topic: String, msgCount: Int, consumer: KafkaConsumer[Array[Byte], String]) {
  def close(): Unit = consumer.close()
}

object KafkaConsumerFixtures extends PerfFixtureHelpers {

  def noopFixtureGen(c: RunTestCommand) = FixtureGen[KafkaConsumerTestFixture](
    c,
    msgCount => {
      KafkaConsumerTestFixture("topic", msgCount, null)
    })

  def filledTopics(c: RunTestCommand) = FixtureGen[KafkaConsumerTestFixture](
    c,
    msgCount => {
      fillTopic(c.filledTopic, c.kafkaHost)
      val consumerJavaProps = new java.util.Properties
      consumerJavaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, c.kafkaHost)
      consumerJavaProps.put(ConsumerConfig.CLIENT_ID_CONFIG, randomId())
      consumerJavaProps.put(ConsumerConfig.GROUP_ID_CONFIG, randomId())
      consumerJavaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      val consumer =
        new KafkaConsumer[Array[Byte], String](consumerJavaProps, new ByteArrayDeserializer, new StringDeserializer)
      consumer.subscribe(Set(c.filledTopic.topic).asJava)
      KafkaConsumerTestFixture(c.filledTopic.topic, msgCount, consumer)
    })
}
