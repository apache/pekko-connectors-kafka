/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.kafka.internal

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.kafka.{ ConsumerSettings, Subscriptions }
import pekko.kafka.scaladsl.Transactional
import pekko.kafka.tests.scaladsl.LogCapturing
import pekko.stream.Materializer
import pekko.stream.scaladsl.Keep
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestKit
import org.apache.kafka.clients.consumer.{ CloseOptions, ConsumerRebalanceListener, ConsumerRecord }
import org.apache.kafka.common.serialization.StringDeserializer
import org.mockito.{ ArgumentCaptor, ArgumentMatchers, Mockito }
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class TransactionalSourceRevokeSpec
    extends TestKit(ActorSystem("TransactionalSourceRevokeSpec"))
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with LogCapturing {

  override def afterAll(): Unit = shutdown(system)

  "Transactional.source" should "not fail the rebalance callback invoked by consumer close after the stage stopped" in {
    val consumerMock = new ConsumerMock[String, String]()
    val mock = consumerMock.mock

    val closed = new CountDownLatch(1)
    val revokeFailure = new AtomicReference[Throwable]()
    // Like the real KafkaConsumer, close() invokes onPartitionsRevoked for the current assignment.
    // If the listener throws, Kafka skips sending LeaveGroup and the member lingers until session timeout.
    Mockito
      .when(mock.close(ArgumentMatchers.any[CloseOptions]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          try {
            val listener = ArgumentCaptor.forClass(classOf[ConsumerRebalanceListener])
            Mockito
              .verify(mock)
              .subscribe(ArgumentMatchers.any[java.util.List[String]], listener.capture())
            listener.getValue.onPartitionsRevoked(mock.assignment())
          } catch {
            case t: Throwable => revokeFailure.set(t)
          } finally closed.countDown()
        }
      })

    val settings = ConsumerSettings
      .create(system, new StringDeserializer, new StringDeserializer)
      .withGroupId("group1")
      .withCloseTimeout(ConsumerMock.closeTimeout)
      .withConsumerFactory(_ => mock)

    val mat = Materializer(system)
    val probe = Transactional
      .source(settings, Subscriptions.topics("topic"))
      .toMat(TestSink[pekko.kafka.ConsumerMessage.TransactionalMessage[String, String]]())(Keep.right)
      .run()(mat)

    probe.request(1)
    consumerMock.enqueue(List(new ConsumerRecord("topic", 1, 0L, "k", "v")))
    probe.expectNext().record.value() shouldBe "v"

    // Abruptly stop the stage, so it is gone before the consumer actor closes the KafkaConsumer.
    mat.shutdown()

    closed.await(10, TimeUnit.SECONDS) shouldBe true
    Option(revokeFailure.get()) shouldBe None
    consumerMock.verifyClosed()
    probe.expectError()
    ()
  }
}
