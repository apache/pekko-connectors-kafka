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
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.kafka.testkit.javadsl.BaseKafkaTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

/**
 * JUnit 5 aka Jupiter base-class with some convenience for accessing a Kafka broker. Extending
 * classes must be annotated with `@TestInstance(Lifecycle.PER_CLASS)` to create a single instance
 * of the test class with `@BeforeAll` and `@AfterAll` annotated methods called by the test
 * framework.
 */
@SuppressWarnings("unchecked")
public abstract class KafkaTest extends BaseKafkaTest {

  /**
   * @deprecated Materializer no longer necessary in Akka 2.6, use
   *     `KafkaTest(ClassicActorSystemProvider, String)` instead, since 2.1.0
   */
  @Deprecated
  protected KafkaTest(ActorSystem system, Materializer mat, String bootstrapServers) {
    super(system, mat, bootstrapServers);
  }

  protected KafkaTest(ClassicActorSystemProvider system, String bootstrapServers) {
    super(system, bootstrapServers);
  }

  @BeforeAll
  public void setupAdmin() {
    setUpAdminClient();
  }

  @AfterAll
  public void cleanUpAdmin() {
    cleanUpAdminClient();
  }

  @AfterEach
  public void checkForStageLeaks() {
    // you might need to configure `stop-timeout` in your `application.conf`
    // as the default of 30s will fail this
    StreamTestKit.assertAllStagesStopped(materializer);
  }
}
