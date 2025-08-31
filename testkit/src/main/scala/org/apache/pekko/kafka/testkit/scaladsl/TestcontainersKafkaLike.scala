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

import org.apache.pekko
import pekko.kafka.testkit.KafkaTestkitTestcontainersSettings
import pekko.kafka.testkit.internal.{ PekkoConnectorsKafkaContainer, SchemaRegistryContainer, TestcontainersKafka }
import org.testcontainers.containers.GenericContainer

/**
 * Uses [[https://www.testcontainers.org/ Testcontainers]] to start a Kafka cluster in a Docker container.
 * This trait will start Kafka only once per test session.  To create a Kafka cluster per test class see
 * [[TestcontainersKafkaPerClassLike]].
 *
 * The Testcontainers dependency has to be added explicitly.
 */
trait TestcontainersKafkaLike extends TestcontainersKafka.Spec {
  override def kafkaPort: Int = TestcontainersKafka.Singleton.kafkaPort
  override def bootstrapServers: String = TestcontainersKafka.Singleton.bootstrapServers
  override def brokerContainers: Vector[PekkoConnectorsKafkaContainer] = TestcontainersKafka.Singleton.brokerContainers
  override def zookeeperContainer: Option[GenericContainer[_]] = TestcontainersKafka.Singleton.zookeeperContainer
  override def schemaRegistryContainer: Option[SchemaRegistryContainer] =
    TestcontainersKafka.Singleton.schemaRegistryContainer
  override def schemaRegistryUrl: String = TestcontainersKafka.Singleton.schemaRegistryUrl
  override def startCluster(): String = TestcontainersKafka.Singleton.startCluster()
  override def startCluster(settings: KafkaTestkitTestcontainersSettings): String =
    TestcontainersKafka.Singleton.startCluster(settings)
  override def stopCluster(): Unit = TestcontainersKafka.Singleton.stopCluster()
  override def startKafka(): Unit = TestcontainersKafka.Singleton.startKafka()
  override def stopKafka(): Unit = TestcontainersKafka.Singleton.stopKafka()

  override def setUp(): Unit = {
    startCluster(testcontainersSettings)
    super.setUp()
  }

  override def cleanUp(): Unit = {
    // do nothing to keep everything running.  testcontainers runs as a daemon and will shut all containers down
    // when the sbt session terminates.
  }
}
