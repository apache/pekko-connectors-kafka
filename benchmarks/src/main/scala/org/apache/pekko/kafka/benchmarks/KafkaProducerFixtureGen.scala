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
import pekko.kafka.benchmarks.PerfFixtureHelpers.FilledTopic
import pekko.kafka.benchmarks.app.RunTestCommand
import org.apache.kafka.clients.producer.KafkaProducer

case class KafkaProducerTestFixture(topic: String,
    msgCount: Int,
    msgSize: Int,
    producer: KafkaProducer[Array[Byte], String],
    numberOfPartitions: Int) {
  def close(): Unit = producer.close()
}

object KafkaProducerFixtures extends PerfFixtureHelpers {

  def noopFixtureGen(c: RunTestCommand) = FixtureGen[KafkaProducerTestFixture](
    c,
    msgCount => {
      KafkaProducerTestFixture("topic", msgCount, c.msgSize, null, c.numberOfPartitions)
    })

  def initializedProducer(c: RunTestCommand) = FixtureGen[KafkaProducerTestFixture](
    c,
    msgCount => {
      val ft = FilledTopic(msgCount = 1, msgSize = c.msgSize, numberOfPartitions = c.numberOfPartitions)
      val rawProducer = createTopic(ft, c.kafkaHost)
      KafkaProducerTestFixture(ft.topic, msgCount, c.msgSize, rawProducer, c.numberOfPartitions)
    })
}
