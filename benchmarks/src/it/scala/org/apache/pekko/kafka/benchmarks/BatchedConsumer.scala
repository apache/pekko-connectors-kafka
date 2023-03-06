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

import org.apache.pekko.kafka.benchmarks.BenchmarksBase.{ topic_1000_100, topic_1000_5000, topic_1000_5000_8 }
import org.apache.pekko.kafka.benchmarks.Timed.runPerfTest
import org.apache.pekko.kafka.benchmarks.app.RunTestCommand

class ApacheKafkaBatchedConsumer extends BenchmarksBase() {
  it should "bench with small messages" in {
    val cmd = RunTestCommand("apache-kafka-batched-consumer", bootstrapServers, topic_1000_100.freshTopic)
    runPerfTest(cmd,
      KafkaConsumerFixtures.filledTopics(cmd),
      KafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }

  it should "bench with normal messages" in {
    val cmd =
      RunTestCommand("apache-kafka-batched-consumer-normal-msg", bootstrapServers, topic_1000_5000.freshTopic)
    runPerfTest(cmd,
      KafkaConsumerFixtures.filledTopics(cmd),
      KafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }

  it should "bench with normal messages and eight partitions" in {
    val cmd =
      RunTestCommand("apache-kafka-batched-consumer-normal-msg-8-partitions",
        bootstrapServers,
        topic_1000_5000_8.freshTopic)
    runPerfTest(cmd,
      KafkaConsumerFixtures.filledTopics(cmd),
      KafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }
}

class PekkoConnectorsKafkaBatchedConsumer extends BenchmarksBase() {

  it should "bench with small messages" in {
    val cmd = RunTestCommand("pekko-connectors-kafka-batched-consumer", bootstrapServers, topic_1000_100)
    runPerfTest(cmd,
      ReactiveKafkaConsumerFixtures.committableSources(cmd),
      ReactiveKafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }

  it should "bench with normal messages" in {
    val cmd = RunTestCommand("pekko-connectors-kafka-batched-consumer-normal-msg", bootstrapServers, topic_1000_5000)
    runPerfTest(cmd,
      ReactiveKafkaConsumerFixtures.committableSources(cmd),
      ReactiveKafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }

  it should "bench with normal messages and eight partitions" in {
    val cmd =
      RunTestCommand("pekko-connectors-kafka-batched-consumer-normal-msg-8-partitions", bootstrapServers,
        topic_1000_5000_8)
    runPerfTest(cmd,
      ReactiveKafkaConsumerFixtures.committableSources(cmd),
      ReactiveKafkaConsumerBenchmarks.consumerAtLeastOnceBatched(batchSize = 1000))
  }
}
