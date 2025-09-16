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

package docs.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.kafka.ProducerMessage.MultiResult
import pekko.kafka.scaladsl.{ Consumer, SendProducer }
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike
import pekko.kafka.{ ConsumerSettings, ProducerMessage, Subscriptions }
import pekko.stream.scaladsl.{ Keep, Sink }
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class SendProducerSpec extends DocsSpecBase with TestcontainersKafkaLike {

  "Simple producer" should "send producer records" in {
    val topic1 = createTopic(1)

    // #record
    val producer = SendProducer.create(producerDefaults)
    try {
      val send: Future[RecordMetadata] = producer
        .send(new ProducerRecord(topic1, "key", "value"))
      // Blocking here for illustration only, you need to handle the future result
      Await.result(send, 2.seconds)
      // #record
      send.futureValue.topic() shouldBe topic1

      val read = consumeHead(consumerDefaults.withGroupId(createGroupId()), topic1)
      read.futureValue shouldBe "value"
      // #record
    } finally {
      Await.result(producer.close(), 1.minute)
    }
    // #record
  }

  it should "send a multi-message (with one record)" in {
    val topic1 = createTopic(1)

    val producer = SendProducer.create(producerDefaults)
    try {
      // #envelope
      val message = ProducerMessage.multi(immutable.Seq(new ProducerRecord(topic1, "key", "value")), "context")
      val send: Future[ProducerMessage.Results[String, String, String]] = producer.sendEnvelope(message)
      // Blocking here for illustration only, you need to handle the future result
      Await.result(send, 2.seconds)
      // #envelope
      val result = send.futureValue
      result match {
        case MultiResult(immutable.Seq(part), "context") =>
          part.metadata.topic() shouldBe topic1
        case other => fail(s"unexpected result $other")
      }

      val read = consumeHead(consumerDefaults.withGroupId(createGroupId()), topic1)
      read.futureValue shouldBe "value"
    } finally {
      producer.close().futureValue shouldBe Done
    }
  }

  it should "send a multi-message (with multiple records)" in {
    val topic1 = createTopic(1)

    // #multiMessage
    val producer = SendProducer.create(producerDefaults)
    try {
      val envelope: ProducerMessage.Envelope[String, String, String] =
        ProducerMessage.multi(immutable.Seq(
            new ProducerRecord(topic1, "key", "value1"),
            new ProducerRecord(topic1, "key", "value2"),
            new ProducerRecord(topic1, "key", "value3")),
          "context")
      val send: Future[ProducerMessage.Results[String, String, String]] = producer.sendEnvelope(envelope)
      // #multiMessage
      val result = send.futureValue
      result match {
        case MultiResult(immutable.Seq(part1, part2, part3), "context") =>
          part1.metadata.topic() shouldBe topic1
        case other => fail(s"unexpected result $other")
      }

      val read = consume(consumerDefaults.withGroupId(createGroupId()), topic1, elements = 3)
      read.futureValue should contain theSameElementsInOrderAs Seq("value1", "value2", "value3")
      // #multiMessage
    } finally {
      Await.result(producer.close(), 1.minute)
    }
    // #multiMessage
  }

  "Mis-configured producer" should "fail the send future" in {
    val topic1 = createTopic(1)
    val producer = SendProducer.create(producerDefaults.withBootstrapServers("unkownhost"))
    try {
      val send = producer.send(new ProducerRecord(topic1, "key", "value"))
      send.failed.futureValue shouldBe a[org.apache.kafka.common.KafkaException]
      send.failed.futureValue.getCause shouldBe a[org.apache.kafka.common.config.ConfigException]
    } finally {
      producer.close().failed.futureValue shouldBe a[org.apache.kafka.common.KafkaException]
    }
  }

  private def consume(consumerSettings: ConsumerSettings[String, String], topic: String, elements: Long) = {
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map(_.value)
      .take(elements)
      .toMat(Sink.seq)(Keep.right)
      .run()
  }

  private def consumeHead(consumerSettings: ConsumerSettings[String, String], topic: String) =
    consume(consumerSettings, topic, 1).map(_.head)
}
