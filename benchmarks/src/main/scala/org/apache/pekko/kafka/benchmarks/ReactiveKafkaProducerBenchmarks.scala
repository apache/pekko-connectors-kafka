/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.benchmarks

import org.apache.pekko.kafka.ConsumerMessage.CommittableMessage
import org.apache.pekko.kafka.ProducerMessage
import org.apache.pekko.kafka.ProducerMessage.{ Result, Results }
import org.apache.pekko.kafka.benchmarks.ReactiveKafkaProducerFixtures.ReactiveKafkaProducerTestFixture
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{ Sink, Source }
import com.codahale.metrics.Meter
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ReactiveKafkaProducerBenchmarks extends LazyLogging {
  val streamingTimeout = 30 minutes
  val logStep = 100000

  type Fixture = ReactiveKafkaConsumerTestFixture[CommittableMessage[Array[Byte], String]]

  /**
   * Iterates over N lazily-generated elements and passes them through a Kafka flow.
   */
  def plainFlow(fixture: ReactiveKafkaProducerTestFixture[Int], meter: Meter)(implicit mat: Materializer): Unit = {
    logger.debug("Creating and starting a stream")
    @volatile var lastPartStart = System.nanoTime()

    val msg = PerfFixtureHelpers.stringOfSize(fixture.msgSize)

    val future = Source(0 to fixture.msgCount)
      .map { number =>
        val partition: Int = (number % fixture.numberOfPartitions).toInt
        ProducerMessage.single(new ProducerRecord[Array[Byte], String](fixture.topic, partition, null, msg), number)
      }
      .via(fixture.flow)
      .map {
        case msg: Result[Array[Byte], String, Int] =>
          meter.mark()
          if (msg.offset % logStep == 0) {
            val lastPartEnd = System.nanoTime()
            val took = (lastPartEnd - lastPartStart).nanos
            logger.info(s"Sent ${msg.offset}, took ${took.toMillis} ms to send last $logStep")
            lastPartStart = lastPartEnd
          }
          msg

        case other: Results[Array[Byte], String, Int] =>
          meter.mark()
          other
      }
      .runWith(Sink.ignore)
    Await.result(future, atMost = streamingTimeout)
    logger.info("Stream finished")
  }

}
