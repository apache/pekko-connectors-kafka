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
import org.apache.kafka.clients.consumer.{ ConsumerConfig, CooperativeStickyAssignor }
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }
import org.scalatest.OptionValues
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class ConsumerSettingsSpec
    extends AnyWordSpec
    with Matchers
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with LogCapturing {

  "ConsumerSettings" must {

    "handle nested kafka-clients properties" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.bootstrap.foo = baz
        pekko.kafka.consumer.kafka-clients.foo = bar
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
      settings.getProperty("bootstrap.servers") should ===("localhost:9092")
      settings.getProperty("client.id") should ===("client1")
      settings.getProperty("foo") should ===("bar")
      settings.getProperty("bootstrap.foo") should ===("baz")
    }

    "handle deserializers defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, None, None)
      settings.getProperty("bootstrap.servers") should ===("localhost:9092")
    }

    "handle deserializers passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.parallelism = 1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
      settings.getProperty("bootstrap.servers") should ===("localhost:9092")
    }

    "handle key deserializer passed as args config and value deserializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, Some(new ByteArrayDeserializer), None)
      settings.getProperty("bootstrap.servers") should ===("localhost:9092")
    }

    "handle value deserializer passed as args config and key deserializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, None, Some(new ByteArrayDeserializer))
      settings.getProperty("bootstrap.servers") should ===("localhost:9092")
    }

    "handle withPartitionAssignmentStrategies" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, None, None)
        .withPartitionAssignmentStrategies(List(
          classOf[org.apache.kafka.clients.consumer.CooperativeStickyAssignor].getName,
          classOf[org.apache.kafka.clients.consumer.StickyAssignor].getName)
        )
      settings.getProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG) should ===(
        "org.apache.kafka.clients.consumer.CooperativeStickyAssignor,org.apache.kafka.clients.consumer.StickyAssignor")
    }

    "handle withPartitionAssignmentStrategyCooperativeStickyAssignor" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, None, None)
        .withPartitionAssignmentStrategyCooperativeStickyAssignor()
      settings.getProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG) should ===(
        "org.apache.kafka.clients.consumer.CooperativeStickyAssignor")
    }

    "filter passwords from kafka-clients properties" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
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

    "mask unregistered Kafka property values only in its diagnostic string" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val key = "example.extension.label"
      val value = "example-label-value"
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
        .withProperty("bootstrap.servers", "localhost:9092")
        .withProperty(key, value)

      val diagnostic = settings.toString
      diagnostic should include(s"($key,[hidden])")
      (diagnostic should not).include(value)
      diagnostic should include("(bootstrap.servers,localhost:9092)")
      settings.properties(key) shouldBe value
    }

    "render a known property even when its value is not valid Kafka config" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
        .withProperty("max.poll.records", "draft-value")

      settings.toString should include("(max.poll.records,draft-value)")
      settings.properties("max.poll.records") shouldBe "draft-value"
    }

    "show well-known extension properties that hold no secrets in its diagnostic string" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
        .withProperty("auto.register.schemas", "false")
        .withProperty("specific.avro.reader", "true")
        .withProperty("basic.auth.credentials.source", "USER_INFO")
        .withProperty("basic.auth.user.info", "example-user:example-secret")

      val diagnostic = settings.toString
      diagnostic should include("(auto.register.schemas,false)")
      diagnostic should include("(specific.avro.reader,true)")
      diagnostic should include("(basic.auth.credentials.source,USER_INFO)")
      diagnostic should include("(basic.auth.user.info,[hidden])")
      (diagnostic should not).include("example-secret")
    }

    "hide the schema registry URL in its diagnostic string" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val url = "http://first:8081,https://example-user:example-secret@second:8081/path?token=example-token"
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
        .withProperty("schema.registry.url", url)

      val diagnostic = settings.toString
      diagnostic should include("(schema.registry.url,[hidden])")
      (diagnostic should not).include("first:8081")
      (diagnostic should not).include("second:8081")
      (diagnostic should not).include("example-user")
      (diagnostic should not).include("example-secret")
      (diagnostic should not).include("example-token")
      settings.properties("schema.registry.url") shouldBe url
    }

    "render a property value that Kafka's validators reject" in {
      val conf = ConfigFactory.load().getConfig("pekko.kafka.consumer")
      val settings = ConsumerSettings(conf, new ByteArrayDeserializer, new StringDeserializer)
        .withProperty("auto.offset.reset", "bogus")

      settings.toString should include("(auto.offset.reset,bogus)")
    }

    "throw IllegalArgumentException if no value deserializer defined" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, None, None)
      }
      exception.getMessage should ===(
        "requirement failed: Value deserializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no value deserializer defined (null case). Key serializer passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, new ByteArrayDeserializer, null)
      }
      exception.getMessage should ===(
        "requirement failed: Value deserializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no value deserializer defined (null case). Key serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.key.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, None, null)
      }
      exception.getMessage should ===(
        "requirement failed: Value deserializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key deserializer defined" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, None, None)
      }
      exception.getMessage should ===(
        "requirement failed: Key deserializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key deserializer defined (null case). Value serializer passed as args config" in {
      val conf = ConfigFactory.parseString("""
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """).withFallback(ConfigFactory.load()).getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, null, new ByteArrayDeserializer)
      }
      exception.getMessage should ===(
        "requirement failed: Key deserializer should be defined or declared in configuration")
    }

    "throw IllegalArgumentException if no key deserializer defined (null case). Value serializer defined in config" in {
      val conf = ConfigFactory
        .parseString(
          """
        pekko.kafka.consumer.kafka-clients.bootstrap.servers = "localhost:9092"
        pekko.kafka.consumer.kafka-clients.value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
        pekko.kafka.consumer.kafka-clients.client.id = client1
        """)
        .withFallback(ConfigFactory.load())
        .getConfig("pekko.kafka.consumer")
      val exception = intercept[IllegalArgumentException] {
        ConsumerSettings(conf, null, None)
      }
      exception.getMessage should ===(
        "requirement failed: Key deserializer should be defined or declared in configuration")
    }

  }

  "Discovery" should {
    val config = ConfigFactory
      .parseString(ConsumerSettingsSpec.DiscoveryConfigSection)
      .withFallback(ConfigFactory.load())
      .resolve()

    "read bootstrap servers from config" in {
      import pekko.kafka.scaladsl.DiscoverySupport
      implicit val actorSystem: ActorSystem = ActorSystem("test", config)

      DiscoverySupport.bootstrapServers(config.getConfig("discovery-consumer")).futureValue shouldBe "cat:1233,dog:1234"

      TestKit.shutdownActorSystem(actorSystem)
    }

    "use enriched settings for consumer creation" in {
      implicit val actorSystem: ActorSystem = ActorSystem("test", config)
      implicit val executionContext: ExecutionContext = actorSystem.dispatcher

      // #discovery-settings
      import org.apache.pekko.kafka.scaladsl.DiscoverySupport

      val consumerConfig = config.getConfig("discovery-consumer")
      val settings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
        .withEnrichAsync(DiscoverySupport.consumerBootstrapServers(consumerConfig))
      // #discovery-settings

      val exception = settings.createKafkaConsumerAsync().failed.futureValue
      exception shouldBe a[org.apache.kafka.common.KafkaException]
      exception.getCause shouldBe a[org.apache.kafka.common.config.ConfigException]
      exception.getCause.getMessage shouldBe "No resolvable bootstrap urls given in bootstrap.servers"

      TestKit.shutdownActorSystem(actorSystem)
    }

  }

}

object ConsumerSettingsSpec {

  val DiscoveryConfigSection =
    s"""
       // #discovery-service
      discovery-consumer: $${pekko.kafka.consumer} {
        service-name = "kafkaService1"
        resolve-timeout = 10 ms
      }
      // #discovery-service
      // #discovery-with-config
      pekko.discovery.method = config
      pekko.discovery.config.services = {
        kafkaService1 = {
          endpoints = [
            { host = "cat", port = 1233 }
            { host = "dog", port = 1234 }
          ]
        }
      }
      // #discovery-with-config
   """
}
