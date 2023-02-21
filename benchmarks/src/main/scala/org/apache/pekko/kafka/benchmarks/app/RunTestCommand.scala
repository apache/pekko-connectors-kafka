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

package org.apache.pekko.kafka.benchmarks.app

import org.apache.pekko.kafka.benchmarks.PerfFixtureHelpers.FilledTopic

case class RunTestCommand(testName: String, kafkaHost: String, filledTopic: FilledTopic) {

  val msgCount = filledTopic.msgCount
  val msgSize = filledTopic.msgSize
  val numberOfPartitions = filledTopic.numberOfPartitions
  val replicationFactor = filledTopic.replicationFactor

}
