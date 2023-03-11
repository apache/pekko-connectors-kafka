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
import pekko.kafka.benchmarks.BenchmarksBase.{ topic_100_100, topic_100_5000 }
import pekko.kafka.benchmarks.Timed.runPerfTest
import pekko.kafka.benchmarks.app.RunTestCommand
import scala.concurrent.duration._

class ApacheKafkaTransactions extends BenchmarksBase() {
  it should "bench with small messages" in {
    val cmd = RunTestCommand("apache-kafka-transactions", bootstrapServers, topic_100_100)
    runPerfTest(cmd,
      KafkaTransactionFixtures.initialize(cmd),
      KafkaTransactionBenchmarks.consumeTransformProduceTransaction(commitInterval = 100.milliseconds))
  }

  it should "bench with normal messages" in {
    val cmd = RunTestCommand("apache-kafka-transactions-normal-msg", bootstrapServers, topic_100_5000)
    runPerfTest(cmd,
      KafkaTransactionFixtures.initialize(cmd),
      KafkaTransactionBenchmarks.consumeTransformProduceTransaction(commitInterval = 100.milliseconds))
  }
}

class PekkoConnectorsKafkaTransactions extends BenchmarksBase() {
  it should "bench with small messages" in {
    val cmd = RunTestCommand("pekko-connectors-kafka-transactions", bootstrapServers, topic_100_100)
    runPerfTest(
      cmd,
      ReactiveKafkaTransactionFixtures.transactionalSourceAndSink(cmd, commitInterval = 100.milliseconds),
      ReactiveKafkaTransactionBenchmarks.consumeTransformProduceTransaction)
  }

  it should "bench with normal messages" in {
    val cmd = RunTestCommand("pekko-connectors-kafka-transactions-normal-msg", bootstrapServers, topic_100_5000)
    runPerfTest(
      cmd,
      ReactiveKafkaTransactionFixtures.transactionalSourceAndSink(cmd, commitInterval = 100.milliseconds),
      ReactiveKafkaTransactionBenchmarks.consumeTransformProduceTransaction)
  }
}
