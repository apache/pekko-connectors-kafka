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

package org.apache.pekko.kafka.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.kafka.tests.scaladsl.LogCapturing
import pekko.kafka.{ ConsumerSettings, Subscriptions }
import pekko.stream.scaladsl.Sink
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.testkit.TestKit
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MisconfiguredConsumerSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with LogCapturing {

  def bootstrapServers = "nowhere:6666"

  "Failing consumer construction" must {
    "be signalled to the stream by single sources" in assertAllStagesStopped {
      val consumerSettings =
        ConsumerSettings(system, new StringDeserializer, new StringDeserializer).withGroupId("group")
      val result = Consumer
        .plainSource(consumerSettings, Subscriptions.topics("topic"))
        .runWith(Sink.head)

      result.failed.futureValue shouldBe a[org.apache.kafka.common.KafkaException]
    }

    "be signalled to the stream by single sources with external offset" in assertAllStagesStopped {
      val consumerSettings =
        ConsumerSettings(system, new StringDeserializer, new StringDeserializer).withGroupId("group")
      val result = Consumer
        .plainSource(consumerSettings, Subscriptions.assignmentWithOffset(new TopicPartition("topic", 0) -> 3123L))
        .runWith(Sink.ignore)

      result.failed.futureValue shouldBe a[org.apache.kafka.common.KafkaException]
    }

    "be signalled to the stream by partitioned sources" in assertAllStagesStopped {
      val consumerSettings =
        ConsumerSettings(system, new StringDeserializer, new StringDeserializer).withGroupId("group")
      val result = Consumer
        .plainPartitionedSource(consumerSettings, Subscriptions.topics("topic"))
        .runWith(Sink.head)

      result.failed.futureValue shouldBe a[org.apache.kafka.common.KafkaException]
    }
  }
}
