/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.testkit.scaladsl

import org.apache.pekko.kafka.testkit.internal.TestFrameworkInterface
import org.scalatest.Suite

abstract class ScalatestKafkaSpec(kafkaPort: Int)
    extends KafkaSpec(kafkaPort)
    with Suite
    with TestFrameworkInterface.Scalatest { this: Suite => }
