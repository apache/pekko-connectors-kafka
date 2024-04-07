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

package org.apache.pekko.kafka.testkit;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.kafka.testkit.internal.TestcontainersKafka;
import org.apache.pekko.kafka.testkit.javadsl.KafkaJunit4Test;
import org.apache.pekko.stream.Materializer;
import org.junit.After;
import org.junit.Before;

/**
 * JUnit 4 base class using [[https://www.testcontainers.org/ Testcontainers]] to start a Kafka
 * broker in a Docker container. The Kafka broker will be kept around across multiple test classes,
 * unless `stopKafka()` is called.
 *
 * <p>The Testcontainers dependency has to be added explicitly.
 */
@SuppressWarnings("unchecked")
public abstract class TestcontainersKafkaJunit4Test extends KafkaJunit4Test {

  private static final KafkaTestkitTestcontainersSettings settings =
      TestcontainersKafka.Singleton().testcontainersSettings();

  /**
   * @deprecated Materializer no longer necessary in Akka 2.6, use
   *     `TestcontainersKafkaJunit4Test(ClassicActorSystemProvider)` instead, since Alpakka Kafka
   *     2.1.0
   */
  @Deprecated
  protected TestcontainersKafkaJunit4Test(ActorSystem system, Materializer mat) {
    super(system, mat, startKafka(settings));
  }

  protected TestcontainersKafkaJunit4Test(ClassicActorSystemProvider system) {
    super(system, startKafka(settings));
  }

  protected TestcontainersKafkaJunit4Test(
      ActorSystem system, KafkaTestkitTestcontainersSettings settings) {
    super(system, startKafka(settings));
  }

  protected static String startKafka(KafkaTestkitTestcontainersSettings settings) {
    return TestcontainersKafka.Singleton().startCluster(settings);
  }

  protected static void stopKafka() {
    TestcontainersKafka.Singleton().stopCluster();
  }

  @Before
  @Override
  public void setUpAdminClient() {
    super.setUpAdminClient();
  }

  @After
  @Override
  public void cleanUpAdminClient() {
    super.cleanUpAdminClient();
  }

  protected String getSchemaRegistryUrl() {
    return TestcontainersKafka.Singleton().getSchemaRegistryUrl();
  }
}
