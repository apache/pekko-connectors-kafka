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

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.kafka.ConsumerMessage.{ PartitionOffsetCommittedMarker, TransactionalMessage }
import pekko.kafka.scaladsl.Transactional
import pekko.kafka.tests.scaladsl.LogCapturing
import pekko.kafka.{ ConsumerSettings, Subscriptions }
import pekko.stream.scaladsl.Keep
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestKit
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/**
 * Kafka continues a rebalance as soon as `onPartitionsRevoked` returns and the new owner of a partition
 * resumes from its last committed offset. The transactional sources must therefore not return from the
 * revoke callback while records of a revoked partition are still in flight, or an open transaction can
 * commit after the new owner has re-read the same records.
 */
class TransactionalSourceRevokeSpec(_system: ActorSystem)
    extends TestKit(_system)
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with LogCapturing {

  import PartitionedSourceSpec._

  def this() = this(ActorSystem("TransactionalSourceRevokeSpec"))

  override def afterAll(): Unit = shutdown(system)

  implicit val ec: ExecutionContext = system.dispatcher

  private val stillBlockedFor = 500.millis

  def consumerSettings(dummy: Dummy): ConsumerSettings[K, V] =
    ConsumerSettings
      .create(system, new StringDeserializer, new StringDeserializer)
      .withGroupId("group")
      .withStopTimeout(10.millis)
      .withCommitTimeout(10.seconds)
      .withConsumerFactory(_ => dummy)

  private def commit(msg: TransactionalMessage[K, V]): Future[Done] = {
    val marker = msg.partitionOffset.asInstanceOf[PartitionOffsetCommittedMarker]
    marker.committedMarker.committed(Map(tp0 -> new OffsetAndMetadata(msg.record.offset() + 1)))
  }

  private def assertRevocationWaitsForCommit(dummy: Dummy, msg: TransactionalMessage[K, V]): Unit = {
    // Revoke every partition. The revoke callback runs on the calling thread, as it does inside `poll`.
    val revocation = Future(dummy.assignWithCallback())

    a[TimeoutException] should be thrownBy Await.result(revocation, stillBlockedFor)

    commit(msg).futureValue shouldBe Done
    revocation.futureValue
  }

  "transactional source" should "not complete partition revocation while records are in flight" in
  assertAllStagesStopped {
    val dummy = new Dummy()

    val (control, sink) = Transactional
      .source(consumerSettings(dummy), Subscriptions.topics(topic))
      .toMat(TestSink())(Keep.both)
      .run()

    dummy.started.futureValue shouldBe Done
    dummy.assignWithCallback(tp0)
    sink.request(1)
    dummy.setNextPollData(tp0 -> singleRecord)
    val msg = sink.expectNext()

    assertRevocationWaitsForCommit(dummy, msg)

    control.shutdown().futureValue shouldBe Done
  }

  "transactional partitioned source" should "not complete partition revocation while records are in flight" in
  assertAllStagesStopped {
    val dummy = new Dummy()

    val (control, sink) = Transactional
      .partitionedSource(consumerSettings(dummy), Subscriptions.topics(topic))
      .toMat(TestSink())(Keep.both)
      .run()

    dummy.started.futureValue shouldBe Done
    dummy.assignWithCallback(tp0)
    val (tp, subSource) = sink.requestNext()
    tp shouldBe tp0
    val subSink = subSource.runWith(TestSink())
    subSink.request(1)
    dummy.setNextPollData(tp0 -> singleRecord)
    val msg = subSink.expectNext()

    assertRevocationWaitsForCommit(dummy, msg)

    subSink.cancel()
    control.shutdown().futureValue shouldBe Done
  }
}
