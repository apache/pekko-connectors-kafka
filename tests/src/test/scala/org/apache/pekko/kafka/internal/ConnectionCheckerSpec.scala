/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.kafka.internal

import org.apache.pekko.actor.{ ActorRef, ActorSystem }
import org.apache.pekko.kafka.Metadata
import org.apache.pekko.kafka.ConnectionCheckerSettings
import org.apache.pekko.kafka.KafkaConnectionFailed
import org.apache.pekko.kafka.tests.scaladsl.LogCapturing
import org.apache.pekko.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.errors.TimeoutException
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class ConnectionCheckerSpec
    extends TestKit(ActorSystem("KafkaConnectionCheckerSpec", ConfigFactory.load()))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "KafkaConnectionChecker" must {

    val retryInterval = 100.millis
    implicit val config: ConnectionCheckerSettings =
      ConnectionCheckerSettings(3, retryInterval, 2d)

    "wait for response and retryInterval before perform new ask" in withCheckerActorRef { checker =>
      expectListTopicsRequest(retryInterval)

      Thread.sleep(retryInterval.toMillis)
      checker ! Metadata.Topics(Success(Map.empty))

      expectListTopicsRequest(retryInterval)
    }

    "exponentially retry on failure and failed after max retries exceeded" in withCheckerActorRef { checker =>
      var interval = retryInterval
      for (_ <- 1 to (config.maxRetries + 1)) {
        expectListTopicsRequest(interval)
        checker ! Metadata.Topics(Failure(new TimeoutException()))
        interval = newExponentialInterval(interval, config.factor)
      }

      watch(checker)
      expectMsgType[KafkaConnectionFailed]
      expectTerminated(checker)
    }

    "return to normal mode if in backoff mode receive Metadata.Topics(success)" in withCheckerActorRef { checker =>
      expectListTopicsRequest(retryInterval)
      checker ! Metadata.Topics(Failure(new TimeoutException()))

      expectListTopicsRequest(newExponentialInterval(retryInterval, config.factor))
      checker ! Metadata.Topics(Success(Map.empty))

      expectListTopicsRequest(retryInterval)
    }
  }

  def newExponentialInterval(previousInterval: FiniteDuration, factor: Double): FiniteDuration =
    (previousInterval * factor).asInstanceOf[FiniteDuration]

  def expectListTopicsRequest(interval: FiniteDuration): Unit = {
    expectNoMessage(interval - 20.millis)
    expectMsg(Metadata.ListTopics)
  }

  def withCheckerActorRef[T](block: ActorRef => T)(implicit config: ConnectionCheckerSettings): T =
    withCheckerActorRef(config)(block)
  def withCheckerActorRef[T](config: ConnectionCheckerSettings)(block: ActorRef => T): T = {
    val checker = childActorOf(ConnectionChecker.props(config))
    val res = block(checker)
    system.stop(watch(checker))
    expectTerminated(checker)
    res
  }

}
