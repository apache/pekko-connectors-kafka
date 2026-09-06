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

package org.apache.pekko.kafka.testkit.scaladsl

import java.time.Duration

import org.apache.pekko
import pekko.kafka.tests.scaladsl.LogCapturing
import pekko.testkit.TestKit
import org.apache.kafka.clients.producer.{ Producer => KProducer }
import org.mockito.{ ArgumentMatchers, Mockito }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaSpecCleanUpSpec extends AnyWordSpec with Matchers with LogCapturing {

  private class TestKafkaSpec extends KafkaSpec(9092) {
    override def bootstrapServers: String = s"localhost:$kafkaPort"
  }

  "KafkaSpec.cleanUp" must {

    "release all resources when closing the test producer fails" in {
      val spec = new TestKafkaSpec
      try {
        val closeFailure = new RuntimeException("test producer close failed")
        val producer = Mockito.mock(classOf[KProducer[String, String]])
        Mockito.doThrow(closeFailure).when(producer).close(ArgumentMatchers.any[Duration])
        spec.testProducer = producer
        spec.setUpAdminClient()

        val thrown = the[RuntimeException] thrownBy spec.cleanUp()
        thrown should ===(closeFailure)

        // the admin client was closed and the actor system was shut down, despite the failing producer close
        an[AssertionError] should be thrownBy spec.adminClient
        Await.result(spec.system.whenTerminated, 10.seconds)
      } finally {
        spec.cleanUpAdminClient()
        TestKit.shutdownActorSystem(spec.system)
      }
    }
  }
}
