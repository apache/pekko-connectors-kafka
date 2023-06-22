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
import pekko.kafka._
import pekko.kafka.scaladsl.Consumer.DrainingControl
import pekko.kafka.scaladsl._
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike
import pekko.stream.{ ActorAttributes, Supervision }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization._

// #spray-imports
import spray.json._
// #spray-imports

// #spray-deser

final case class SampleData(name: String, value: Int)

object SampleDataSprayProtocol extends DefaultJsonProtocol {
  implicit val sampleDataProtocol: RootJsonFormat[SampleData] = jsonFormat2(SampleData)
}

import SampleDataSprayProtocol._
// #spray-deser

class SerializationSpec extends DocsSpecBase with TestcontainersKafkaLike {

  "Deserialization in map" should "be documented" in assertAllStagesStopped {
    val group = createGroupId()
    val topic = createTopic()

    val sample = SampleData("Viktor", 54)
    val samples = List(sample, sample, sample)

    awaitProduce(
      produceString(topic, List("{faulty JSON data") ++ samples.map(_.toJson.compactPrint)))

    val consumerSettings = consumerDefaults.withGroupId(group)

    // #spray-deser

    val resumeOnParsingException = ActorAttributes.supervisionStrategy {
      case _: spray.json.JsonParser.ParsingException => Supervision.Resume
      case _                                         => Supervision.stop
    }

    val consumer = Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map { consumerRecord =>
        val value = consumerRecord.value()
        val sampleData = value.parseJson.convertTo[SampleData]
        sampleData
      }
      .withAttributes(resumeOnParsingException)
      // #spray-deser
      .take(samples.size.toLong)
      // #spray-deser
      .toMat(Sink.seq)(DrainingControl.apply)
      .run()
    // #spray-deser

    consumer.isShutdown.futureValue should be(Done)
    consumer.drainAndShutdown().futureValue should be(samples)
  }

  "Protobuf use" should "be documented" in assertAllStagesStopped {
    // #protobuf-imports
    // the Protobuf generated class
    import docs.scaladsl.proto.Order

    // #protobuf-imports
    val group = createGroupId()
    val topic = createTopic()

    val sample = Order(id = "789465")
    val samples = List(sample, sample, sample)

    // #protobuf-serializer
    val producerSettings: ProducerSettings[String, Array[Byte]] = // ...
      // #protobuf-serializer
      producerDefaults(new StringSerializer(), new ByteArraySerializer())

    // #protobuf-serializer

    val producerCompletion =
      Source(samples)
        .map(order => new ProducerRecord(topic, order.id, order.toByteArray))
        .runWith(Producer.plainSink(producerSettings))
    // #protobuf-serializer

    // #protobuf-deserializer
    val resumeOnParsingException = ActorAttributes.supervisionStrategy {
      case _: com.google.protobuf.InvalidProtocolBufferException => Supervision.Resume
      case _                                                     => Supervision.stop
    }

    val consumerSettings: ConsumerSettings[String, Array[Byte]] = // ...
      // #protobuf-deserializer
      consumerDefaults(new StringDeserializer, new ByteArrayDeserializer).withGroupId(group)
    // #protobuf-deserializer

    val consumer = Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map { consumerRecord =>
        Order.parseFrom(consumerRecord.value())
      }
      .withAttributes(resumeOnParsingException)
      // #protobuf-deserializer
      .take(samples.size.toLong)
      // #protobuf-deserializer
      .toMat(Sink.seq)(DrainingControl.apply)
      .run()
    // #protobuf-deserializer

    producerCompletion.futureValue shouldBe Done
    consumer.isShutdown.futureValue should be(Done)
    consumer.drainAndShutdown().futureValue should be(samples)
  }

}
