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
import pekko.actor.{ ActorRef, ActorSystem, Props }
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class KafkaAsyncConsumerCommitterRefSpec
    extends TestKit(ActorSystem("KafkaAsyncConsumerCommitterRefSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  private def committer(actorRef: ActorRef, timeout: FiniteDuration) =
    new KafkaAsyncConsumerCommitterRef(actorRef, timeout)(system.dispatcher)

  "KafkaAsyncConsumerCommitterRef equals/hashCode" should {

    "be equal for same actorRef and timeout" in {
      val ref = system.actorOf(Props.empty)
      val a = committer(ref, 5.seconds)
      val b = committer(ref, 5.seconds)
      a shouldEqual b
      a.hashCode() shouldEqual b.hashCode()
    }

    "not be equal for different actorRef" in {
      val ref1 = system.actorOf(Props.empty)
      val ref2 = system.actorOf(Props.empty)
      val a = committer(ref1, 5.seconds)
      val b = committer(ref2, 5.seconds)
      (a should not).equal(b)
    }

    "not be equal for different timeout" in {
      val ref = system.actorOf(Props.empty)
      val a = committer(ref, 5.seconds)
      val b = committer(ref, 10.seconds)
      (a should not).equal(b)
    }

    "not be equal to a non-KafkaAsyncConsumerCommitterRef" in {
      val ref = system.actorOf(Props.empty)
      val a = committer(ref, 5.seconds)
      (a should not).equal("not a committer")
    }

    "have consistent hashCode for equal instances" in {
      val ref = system.actorOf(Props.empty)
      val a = committer(ref, 150.millis)
      val b = committer(ref, 150.millis)
      a.hashCode() shouldEqual b.hashCode()
    }

    "have different hashCode for different actorRef (probabilistic)" in {
      val ref1 = system.actorOf(Props.empty)
      val ref2 = system.actorOf(Props.empty)
      val a = committer(ref1, 5.seconds)
      val b = committer(ref2, 5.seconds)
      // Not guaranteed but extremely likely
      (a.hashCode() should not).equal(b.hashCode())
    }
  }
}
