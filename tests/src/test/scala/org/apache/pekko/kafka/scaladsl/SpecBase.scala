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

package org.apache.pekko.kafka.scaladsl

import org.apache.pekko
import pekko.kafka.Repeated
import pekko.kafka.tests.scaladsl.LogCapturing
// #testkit
import org.apache.pekko
import pekko.kafka.testkit.scaladsl.ScalatestKafkaSpec
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

abstract class SpecBase(kafkaPort: Int)
    extends ScalatestKafkaSpec(kafkaPort)
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    // #testkit
    with LogCapturing
    with IntegrationPatience
    with Repeated
    // #testkit
    with Eventually {

  protected def this() = this(kafkaPort = -1)
}

// #testkit

// #testcontainers
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike

class TestcontainersSampleSpec extends SpecBase with TestcontainersKafkaLike {
  // ...
}
// #testcontainers

// #testcontainers-settings
import pekko.kafka.testkit.KafkaTestkitTestcontainersSettings
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaPerClassLike

class TestcontainersNewSettingsSampleSpec extends SpecBase with TestcontainersKafkaPerClassLike {

  override val testcontainersSettings = KafkaTestkitTestcontainersSettings(system)
    .withNumBrokers(3)
    .withInternalTopicsReplicationFactor(2)
    .withConfigureKafka { brokerContainers =>
      brokerContainers.foreach(_.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false"))
    }

  // ...
}
// #testcontainers-settings
