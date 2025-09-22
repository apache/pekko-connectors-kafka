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

package org.apache.pekko.kafka.testkit.javadsl;

import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.junit.After;
import org.junit.Before;

/** JUnit 4 base-class with some convenience for accessing a Kafka broker. */
@SuppressWarnings("unchecked")
public abstract class KafkaJunit4Test extends BaseKafkaTest {

  protected KafkaJunit4Test(ClassicActorSystemProvider system, String bootstrapServers) {
    super(system, bootstrapServers);
  }

  @Before
  public void setUpAdmin() {
    setUpAdminClient();
  }

  @After
  public void cleanUpAdmin() {
    cleanUpAdminClient();
  }

  @After
  public void checkForStageLeaks() {
    // you might need to configure `stop-timeout` in your `application.conf`
    // as the default of 30s will fail this
    StreamTestKit.assertAllStagesStopped(materializer);
  }
}
