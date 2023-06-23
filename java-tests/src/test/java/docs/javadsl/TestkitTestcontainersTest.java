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

package docs.javadsl;

// #testcontainers-settings
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.kafka.testkit.KafkaTestkitTestcontainersSettings;
import org.apache.pekko.kafka.testkit.TestcontainersKafkaTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestkitTestcontainersTest extends TestcontainersKafkaTest {

  private static final ActorSystem system = ActorSystem.create("TestkitTestcontainersTest");

  private static KafkaTestkitTestcontainersSettings testcontainersSettings =
      KafkaTestkitTestcontainersSettings.create(system)
          .withNumBrokers(3)
          .withInternalTopicsReplicationFactor(2)
          .withConfigureKafkaConsumer(
              brokerContainers ->
                  brokerContainers.forEach(
                      b -> b.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")));

  TestkitTestcontainersTest() {
    // this will only start a new cluster if it has not already been started.
    //
    // you must stop the cluster in the afterClass implementation if you want to create a cluster
    // per test class
    // using (TestInstance.Lifecycle.PER_CLASS)
    super(system, testcontainersSettings);
  }

  // ...

  // omit this implementation if you want the cluster to stay up for all your tests
  @AfterAll
  void afterClass() {
    TestcontainersKafkaTest.stopKafka();
  }
}
// #testcontainers-settings
