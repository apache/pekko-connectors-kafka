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

package org.apache.pekko.kafka

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.kafka.tests.scaladsl.LogCapturing
import pekko.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }

class ProducerSettingsSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with LogCapturing {

  "ProducerSettings" must {

    // TODO: Are we going to change configuration?
    "handle serializers defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.key.serializer = org.apache.kafka.common.serialization.StringSerializer
        pekko.kafka.producer.kafka-clients.value.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val settings = ProducerSettings(conf, None, None)
      settings.properties("bootstrap.servers") should ===("localhost:9092")
    }

    "handle serializers passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.producer")
      val settings = ProducerSettings(conf, new ByteArraySerializer, new StringSerializer)
      settings.properties("bootstrap.servers") should ===("localhost:9092")
    }

    "handle key serializer passed as args config and value serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.value.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val settings = ProducerSettings(conf, Some(new ByteArraySerializer), None)
      settings.properties("bootstrap.servers") should ===("localhost:9092")
    }

    "handle value serializer passed as args config and key serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.key.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val settings = ProducerSettings(conf, None, Some(new ByteArraySerializer))
      settings.properties("bootstrap.servers") should ===("localhost:9092")
    }

    "filter passwords from kafka-clients properties" in {
      val conf = ConfigFactory.load().getConfig(ProducerSettings.configPath)
      val settings = ProducerSettings(conf, new ByteArraySerializer, new StringSerializer)
        .withProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "hemligt")
        .withProperty("ssl.truststore.password", "geheim")
        .withProperty("ssl.keystore.key", "schlussel")
      val s = settings.toString
      s should include(SslConfigs.SSL_KEY_PASSWORD_CONFIG)
      (s should not).include("hemligt")
      s should include("ssl.truststore.password")
      (s should not).include("geheim")
      s should include("ssl.keystore.key")
      (s should not).include("schlussel")
    }

    "throw IllegalArgumentException if no value serializer defined" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.key.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, None, None)
      }
      exception.getMessage should ===(
        "requirement failed: Value serializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no value serializer defined (null case). Key serializer passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, new ByteArraySerializer, null)
      }
      exception.getMessage should ===(
        "requirement failed: Value serializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no value serializer defined (null case). Key serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.key.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, None, null)
      }
      exception.getMessage should ===(
        "requirement failed: Value serializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key serializer defined" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.value.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, None, None)
      }
      exception.getMessage should ===(
        "requirement failed: Key serializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key serializer defined (null case). Value serializer passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, null, new ByteArraySerializer)
      }
      exception.getMessage should ===(
        "requirement failed: Key serializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key serializer defined (null case). Value serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.producer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.producer.kafka-clients.parallelism = 1
        pekko.kafka.producer.kafka-clients.value.serializer = org.apache.kafka.common.serialization.StringSerializer
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.producer")
      val exception = intercept[IllegalArgumentException] {
        ProducerSettings(conf, null, None)
      }
      exception.getMessage should ===(
        "requirement failed: Key serializer should be defined or declared in configuration")
    }

  }

  "Discovery" should {
    val config = ConfigFactory
      .parseString(ProducerSettingsSpec.DiscoveryConfigSection)
      .withFallback(ConfigFactory.load())
      .resolve()

    "use enriched settings for consumer creation" in {
      implicit val actorSystem = ActorSystem("test", config)

      // #discovery-settings
      import org.apache.pekko.kafka.scaladsl.DiscoverySupport

      val producerConfig = config.getConfig("discovery-producer")
      val settings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
        .withEnrichAsync(DiscoverySupport.producerBootstrapServers(producerConfig))
      // #discovery-settings

      val exception = settings.createKafkaProducerAsync()(actorSystem.dispatcher).failed.futureValue
      exception shouldBe a[org.apache.kafka.common.KafkaException]
      exception.getCause shouldBe a[org.apache.kafka.common.config.ConfigException]
      exception.getCause.getMessage shouldBe "No resolvable bootstrap urls given in bootstrap.servers"
      TestKit.shutdownActorSystem(actorSystem)
    }

    "fail if using non-async creation with enrichAsync" in {
      implicit val actorSystem = ActorSystem("test", config)

      import pekko.kafka.scaladsl.DiscoverySupport

      val producerConfig = config.getConfig("discovery-producer")
      val settings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
        .withEnrichAsync(DiscoverySupport.producerBootstrapServers(producerConfig))

      val exception = intercept[IllegalStateException] {
        settings.createKafkaProducer()
      }
      exception shouldBe a[IllegalStateException]
      TestKit.shutdownActorSystem(actorSystem)
    }
  }

}

object ProducerSettingsSpec {
  val DiscoveryConfigSection =
    s"""
        // #discovery-service
        discovery-producer: $${pekko.kafka.producer} {
          service-name = "kafkaService1"
          resolve-timeout = 10 ms
        }
        // #discovery-service
        pekko.discovery.method = config
        pekko.discovery.config.services = {
          kafkaService1 = {
            endpoints = [
              { host = "cat", port = 1233 }
              { host = "dog", port = 1234 }
            ]
          }
        }
        """
}
