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
import pekko.kafka.benchmarks.Timed.runPerfTest
import pekko.kafka.benchmarks.app.RunTestCommand

import BenchmarksBase._

class RawKafkaCommitEveryPollConsumer extends BenchmarksBase() {
  private val prefix = "apache-kafka-batched-no-pausing-"

  it should "bench with small messages" in {
    val cmd = RunTestCommand(prefix + "consumer", bootstrapServers, topic_1000_100)
    runPerfTest(cmd,
      KafkaConsumerFixtures.filledTopics(cmd),
      KafkaConsumerBenchmarks.consumerAtLeastOnceCommitEveryPoll())
  }

// These are not plotted anyway
//  it should "bench with normal messages" in {
//    val cmd = RunTestCommand(prefix + "consumer-normal-msg", bootstrapServers, topic_1000_5000)
//    runPerfTest(cmd,
//                KafkaConsumerFixtures.filledTopics(cmd),
//                KafkaConsumerBenchmarks.consumerAtLeastOnceCommitEveryPoll())
//  }
//
//  it should "bench with normal messages and eight partitions" in {
//    val cmd = RunTestCommand(prefix + "consumer-normal-msg-8-partitions",
//                             bootstrapServers,
//                             topic_1000_5000_8)
//    runPerfTest(cmd,
//                KafkaConsumerFixtures.filledTopics(cmd),
//                KafkaConsumerBenchmarks.consumerAtLeastOnceCommitEveryPoll())
//  }
}

class PekkoConnectorsCommitAndForgetConsumer extends BenchmarksBase() {
  val prefix = "pekko-connectors-kafka-commit-and-forget-"

  it should "bench with small messages" in {
    val cmd = RunTestCommand(prefix + "consumer", bootstrapServers, topic_1000_100)
    runPerfTest(cmd,
      ReactiveKafkaConsumerFixtures.committableSources(cmd),
      ReactiveKafkaConsumerBenchmarks.consumerCommitAndForget(commitBatchSize = 1000))
  }

// These are not plotted anyway
//  it should "bench with normal messages" in {
//    val cmd = RunTestCommand(prefix + "normal-msg", bootstrapServers, topic_1000_5000)
//    runPerfTest(cmd,
//                ReactiveKafkaConsumerFixtures.committableSources(cmd),
//                ReactiveKafkaConsumerBenchmarks.consumerCommitAndForget(commitBatchSize = 1000))
//  }
//
//  it should "bench with normal messages and eight partitions" in {
//    val cmd = RunTestCommand(prefix + "normal-msg-8-partitions",
//                             bootstrapServers,
//                             topic_1000_5000_8)
//    runPerfTest(cmd,
//                ReactiveKafkaConsumerFixtures.committableSources(cmd),
//                ReactiveKafkaConsumerBenchmarks.consumerCommitAndForget(commitBatchSize = 1000))
//  }
}
