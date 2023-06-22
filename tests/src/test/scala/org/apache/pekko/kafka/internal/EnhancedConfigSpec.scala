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

package org.apache.pekko.kafka.internal

import org.apache.pekko.kafka.tests.scaladsl.LogCapturing
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class EnhancedConfigSpec extends AnyWordSpec with Matchers with LogCapturing {

  "EnhancedConfig" must {

    "parse infinite durations" in {
      val conf = ConfigFactory.parseString("foo-interval = infinite")
      val interval = ConfigSettings.getPotentiallyInfiniteDuration(conf, "foo-interval")
      interval should ===(Duration.Inf)
    }

    "parse finite durations" in {
      val conf = ConfigFactory.parseString("foo-interval = 1m")
      val interval = ConfigSettings.getPotentiallyInfiniteDuration(conf, "foo-interval")
      interval should ===(1.minute)
    }

  }

}
