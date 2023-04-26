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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.kafka.ConsumerSettings;
import org.apache.pekko.kafka.ConsumerSettingsSpec$;
// #discovery-settings
import org.apache.pekko.kafka.javadsl.DiscoverySupport;
// #discovery-settings
import org.apache.pekko.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

public class ConsumerSettingsTest {

  @Test
  public void discoverySetup() throws Exception {
    Config config =
        ConfigFactory.parseString(ConsumerSettingsSpec$.MODULE$.DiscoveryConfigSection())
            .withFallback(ConfigFactory.load())
            .resolve();
    ActorSystem system = ActorSystem.create("ConsumerSettingsTest", config);

    // #discovery-settings

    Config consumerConfig = system.settings().config().getConfig("discovery-consumer");
    ConsumerSettings<String, String> settings =
        ConsumerSettings.create(consumerConfig, new StringDeserializer(), new StringDeserializer())
            .withEnrichCompletionStage(
                DiscoverySupport.consumerBootstrapServers(consumerConfig, system));
    // #discovery-settings
    TestKit.shutdownActorSystem(system);
  }
}
