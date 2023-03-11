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

package org.apache.pekko.kafka.scaladsl

import org.apache.pekko
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike
import pekko.kafka.Subscriptions
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.stream.testkit.scaladsl.TestSink
import org.apache.kafka.common.TopicPartition
import org.scalatest.Inside
import org.scalatest.concurrent.IntegrationPatience

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class TimestampSpec extends SpecBase with TestcontainersKafkaLike with Inside with IntegrationPatience {

  "Kafka connector" must {
    "begin consuming from the given timestamp of the topic" in {
      assertAllStagesStopped {
        val topic = createTopic(1)
        val group = createGroupId(1)

        val now = System.currentTimeMillis()
        Await.result(produceTimestamped(topic, (1 to 100).zip(now to (now + 100))), remainingOrDefault)

        val consumerSettings = consumerDefaults.withGroupId(group)
        val consumer = consumerSettings.createKafkaConsumerAsync().futureValue
        val partitions = consumer.partitionsFor(topic).asScala.map { t =>
          new TopicPartition(t.topic(), t.partition())
        }
        val topicsAndTs = Subscriptions.assignmentOffsetsForTimes(partitions.map(_ -> (now + 50)).toSeq: _*)

        val probe = Consumer
          .plainSource(consumerSettings, topicsAndTs)
          .map(_.value())
          .runWith(TestSink.probe)

        probe
          .request(50)
          .expectNextN((51 to 100).map(_.toString))

        probe.cancel()
      }
    }

    "handle topic that has no messages by timestamp" in {
      assertAllStagesStopped {
        val topic = createTopic(1)
        val group = createGroupId(1)

        val now = System.currentTimeMillis()

        val consumerSettings = consumerDefaults.withGroupId(group)
        val consumer = consumerSettings.createKafkaConsumerAsync().futureValue
        val partitions = consumer.partitionsFor(topic).asScala.map { t =>
          new TopicPartition(t.topic(), t.partition())
        }
        val topicsAndTs = Subscriptions.assignmentOffsetsForTimes(partitions.map(_ -> (now + 50)).toSeq: _*)

        val probe = Consumer
          .plainSource(consumerSettings, topicsAndTs)
          .runWith(TestSink.probe)

        probe.ensureSubscription()
        probe.expectNoMessage(200.millis)
        probe.cancel()
      }
    }

    "handle non existing topic" in {
      assertAllStagesStopped {
        val group = createGroupId(1)

        val now = System.currentTimeMillis()

        val consumerSettings = consumerDefaults.withGroupId(group)
        val topicsAndTs = Subscriptions.assignmentOffsetsForTimes(new TopicPartition("non-existing-topic", 0) -> now)

        val probe = Consumer
          .plainSource(consumerSettings, topicsAndTs)
          .runWith(TestSink.probe)

        probe.ensureSubscription()
        probe.expectNoMessage(200.millis)
        probe.cancel()
      }
    }
  }
}
