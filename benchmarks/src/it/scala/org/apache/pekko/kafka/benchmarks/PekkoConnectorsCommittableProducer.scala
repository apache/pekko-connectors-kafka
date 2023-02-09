/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.benchmarks

import org.apache.pekko.kafka.benchmarks.BenchmarksBase.{ topic_100_100, topic_100_5000 }
import org.apache.pekko.kafka.benchmarks.Timed.runPerfTest
import org.apache.pekko.kafka.benchmarks.app.RunTestCommand

/**
 * Compares the `CommittingProducerSinkStage` with the composed implementation of `Producer.flexiFlow` and `Committer.sink`.
 */
class PekkoConnectorsCommittableProducer extends BenchmarksBase() {
  it should "bench composed sink with 100b messages" in {
    val cmd = RunTestCommand("pekko-connectors-committable-producer-composed", bootstrapServers, topic_100_100)
    runPerfTest(
      cmd,
      PekkoConnectorsCommittableSinkFixtures.composedSink(cmd),
      PekkoConnectorsCommittableSinkBenchmarks.run)
  }

  it should "bench composed sink with 5000b messages" in {
    val cmd = RunTestCommand("pekko-connectors-committable-producer-composed-5000b", bootstrapServers, topic_100_5000)
    runPerfTest(
      cmd,
      PekkoConnectorsCommittableSinkFixtures.composedSink(cmd),
      PekkoConnectorsCommittableSinkBenchmarks.run)
  }

  it should "bench `Producer.committableSink` with 100b messages" in {
    val cmd = RunTestCommand("pekko-connectors-committable-producer", bootstrapServers, topic_100_100)
    runPerfTest(
      cmd,
      PekkoConnectorsCommittableSinkFixtures.producerSink(cmd),
      PekkoConnectorsCommittableSinkBenchmarks.run)
  }

  it should "bench `Producer.committableSink` with 5000b messages" in {
    val cmd = RunTestCommand("pekko-connectors-committable-producer-5000b", bootstrapServers, topic_100_5000)
    runPerfTest(
      cmd,
      PekkoConnectorsCommittableSinkFixtures.producerSink(cmd),
      PekkoConnectorsCommittableSinkBenchmarks.run)
  }
}
