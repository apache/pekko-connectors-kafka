/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.scaladsl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.tests.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.pekko.testkit.TestKit
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MisconfiguredProducerSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with LogCapturing {

  "Failing producer construction" must {
    "fail stream appropriately" in assertAllStagesStopped {
      val producerSettings =
        ProducerSettings(system, new StringSerializer, new StringSerializer)
          .withBootstrapServers("invalid-bootstrap-server")

      val completion = Source
        .single(new ProducerRecord[String, String]("topic", "key", "value"))
        .runWith(Producer.plainSink(producerSettings))

      val exception = completion.failed.futureValue
      exception shouldBe a[org.apache.kafka.common.KafkaException]
      exception.getMessage shouldBe "Failed to construct kafka producer"
    }
  }
}
