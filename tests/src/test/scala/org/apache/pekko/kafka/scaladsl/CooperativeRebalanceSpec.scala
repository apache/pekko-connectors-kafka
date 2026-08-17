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

package org.apache.pekko.kafka.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.kafka._
import pekko.kafka.scaladsl.Consumer.Control
import pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike
import pekko.stream.scaladsl.{ Keep, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestProbe
import org.apache.kafka.clients.consumer.{ ConsumerConfig, ConsumerRecord }
import org.apache.kafka.common.TopicPartition
import org.scalatest.Inside

import scala.concurrent.duration._
import scala.util.Random

/**
 * With the cooperative rebalance protocol `onPartitionsRevoked` is only invoked on members that
 * actually revoke partitions, while `onPartitionsAssigned` is invoked on every member in every
 * rebalance, possibly with an empty set. These tests reproduce rebalances where the revoke
 * callback is skipped and assert that buffered records of partitions that remain assigned are
 * still delivered (at-least-once).
 *
 * Reproduces the scenario of https://github.com/apache/pekko-connectors-kafka/issues/616:
 *  1. a partition is revoked from consumer 1 (its `lastRevoked` state becomes non-empty),
 *  2. the partition is later re-assigned to consumer 1,
 *  3. an unrelated member joins, completing a rebalance in which consumer 1 revokes nothing
 *     (`onPartitionsRevoked` is not invoked) and gains nothing (`onPartitionsAssigned` with an
 *     empty set) while records for the re-assigned partition sit in the source stage buffer.
 *
 * The test runs against `plainSource` and `plainPartitionedSource` (flattened with
 * `flatMapMerge` so both shapes expose a single stream of records) as the underlying stages
 * implement the revoked-buffer bookkeeping independently. The merge stage of the flattened
 * variant buffers a few records of its own that survive a revoke as duplicates, so the tests
 * assert complete delivery rather than an exact sequence.
 */
class CooperativeRebalanceSpec extends SpecBase with TestcontainersKafkaLike with Inside {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(30.seconds, 500.millis)

  val partition1 = 1
  val consumerClientId1 = "consumer-1"
  val consumerClientId2 = "consumer-2"
  val consumerClientId3 = "consumer-3"

  private def cooperativeSettings(group: String) =
    consumerDefaults
      .withProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500")
      .withProperty(
        ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        classOf[CooperativePekkoConnectorsAssignor].getName)
      .withGroupId(group)

  private def awaitAssigned(rebalanceActor: TestProbe,
      subscription: AutoSubscription,
      tps: Set[TopicPartition]): Unit =
    rebalanceActor.fishForMessage(10.seconds) {
      case TopicPartitionsAssigned(`subscription`, assigned) if assigned == tps  => true
      case TopicPartitionsAssigned(`subscription`, assigned) if assigned.isEmpty => false
      case _                                                                     => false
    }

  sealed trait SourceCase {
    def label: String
    def source(settings: ConsumerSettings[String, String],
        subscription: AutoSubscription): Source[ConsumerRecord[String, String], Control]
  }

  case object PlainCase extends SourceCase {
    override val label = "plain source"
    override def source(settings: ConsumerSettings[String, String],
        subscription: AutoSubscription): Source[ConsumerRecord[String, String], Control] =
      Consumer.plainSource(settings, subscription)
  }

  case object PartitionedCase extends SourceCase {
    override val label = "partitioned source"
    override def source(settings: ConsumerSettings[String, String],
        subscription: AutoSubscription): Source[ConsumerRecord[String, String], Control] =
      Consumer
        .plainPartitionedSource(settings, subscription)
        .flatMapMerge(breadth = 8, { case (_, records) => records })
  }

  "Buffered records of partitions that stay assigned" must {

    List(PlainCase, PartitionedCase).foreach { mode =>
      s"be delivered after a rebalance without revocation (${mode.label})" in assertAllStagesStopped {
        val count = 100L
        val topicSuffix = Random.nextInt()
        val topic1 = createTopic(topicSuffix, partitions = 2)
        val group1 = createGroupId(1)
        val tp0 = new TopicPartition(topic1, partition0)
        val tp1 = new TopicPartition(topic1, partition1)
        val consumerSettings = cooperativeSettings(group1)

        def joinConsumer(clientId: String) = {
          val rebalanceActor = TestProbe()
          val subscription = Subscriptions.topics(topic1).withRebalanceListener(rebalanceActor.ref)
          val (control, probe) = Consumer
            .plainSource(consumerSettings.withClientId(clientId), subscription)
            .toMat(TestSink())(Keep.both)
            .run()
          (control, probe, rebalanceActor, subscription)
        }

        awaitProduce(produce(topic1, 0 to count.toInt, partition1))

        PekkoConnectorsAssignor.clientIdToPartitionMap.set(Map(consumerClientId1 -> Set(tp0, tp1)))

        val probe1rebalanceActor = TestProbe()
        val probe1subscription = Subscriptions.topics(topic1).withRebalanceListener(probe1rebalanceActor.ref)
        val (control1, probe1) = mode
          .source(consumerSettings.withClientId(consumerClientId1), probe1subscription)
          .toMat(TestSink())(Keep.both)
          .run()

        probe1rebalanceActor.expectMsg(TopicPartitionsAssigned(probe1subscription, Set(tp0, tp1)))
        probe1.requestNext()

        PekkoConnectorsAssignor.clientIdToPartitionMap.set(
          Map(consumerClientId1 -> Set(tp0), consumerClientId2 -> Set(tp1)))
        val (control2, probe2, probe2rebalanceActor, probe2subscription) = joinConsumer(consumerClientId2)

        probe1rebalanceActor.expectMsg(TopicPartitionsRevoked(probe1subscription, Set(tp1)))
        awaitAssigned(probe2rebalanceActor, probe2subscription, Set(tp1))

        PekkoConnectorsAssignor.clientIdToPartitionMap.set(Map(consumerClientId1 -> Set(tp0, tp1)))
        probe2.cancel()
        control2.isShutdown.futureValue shouldBe Done
        awaitAssigned(probe1rebalanceActor, probe1subscription, Set(tp1))

        probe1.requestNext()

        PekkoConnectorsAssignor.clientIdToPartitionMap.set(
          Map(consumerClientId1 -> Set(tp0, tp1), consumerClientId3 -> Set.empty[TopicPartition]))
        val (control3, probe3, probe3rebalanceActor, probe3subscription) = joinConsumer(consumerClientId3)

        probe1rebalanceActor.expectMsg(TopicPartitionsAssigned(probe1subscription, Set.empty))
        probe3rebalanceActor.expectMsg(TopicPartitionsAssigned(probe3subscription, Set.empty))

        // give the asynchronous buffer filter of the rebalance a chance to apply before demanding
        probe1.expectNoMessage(500.millis)

        probe1.request(count * 3)
        val values = probe1.receiveWithin(5.seconds).map(_.value)
        values should contain allElementsOf (1 to count.toInt).map(_.toString)

        probe1.cancel()
        probe3.cancel()
        control1.isShutdown.futureValue shouldBe Done
        control3.isShutdown.futureValue shouldBe Done
      }
    }
  }
}
