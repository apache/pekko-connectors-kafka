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

package org.apache.pekko.kafka.testkit.scaladsl

import org.apache.pekko.kafka.testkit.internal.TestcontainersKafka

/**
 * Uses [[https://www.testcontainers.org/ Testcontainers]] to start a Kafka broker in a Docker container once per class.
 * The Testcontainers dependency has to be added explicitly.
 */
trait TestcontainersKafkaPerClassLike extends TestcontainersKafka.Spec {
  override def setUp(): Unit = {
    startCluster()
    super.setUp()
  }

  override def cleanUp(): Unit = {
    super.cleanUp()
    stopCluster()
  }
}
