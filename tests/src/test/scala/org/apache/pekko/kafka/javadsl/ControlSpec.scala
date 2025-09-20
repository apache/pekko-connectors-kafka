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

package org.apache.pekko.kafka.javadsl

import java.util
import java.util.concurrent.{ CompletionStage, Executor, Executors }
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.pekko
import pekko.Done
import pekko.kafka.internal.ConsumerControlAsJava
import pekko.kafka.tests.scaladsl.LogCapturing
import scala.jdk.FutureConverters._
import org.apache.kafka.common.{ Metric, MetricName }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.language.reflectiveCalls

object ControlSpec {

  trait WithShutdownCalled {
    def shutdownCalled: AtomicBoolean
  }

  def createControl(stopFuture: Future[Done] = Future.successful(Done),
      shutdownFuture: Future[Done] = Future.successful(Done)): Consumer.Control with WithShutdownCalled = {
    val control = new pekko.kafka.scaladsl.ControlSpec.ControlImpl(stopFuture, shutdownFuture)
    val wrapped = new ConsumerControlAsJava(control)
    new Consumer.Control with WithShutdownCalled {
      override def shutdownCalled: AtomicBoolean = control.shutdownCalled

      override def stop(): CompletionStage[Done] = wrapped.stop()

      override def shutdown(): CompletionStage[Done] = wrapped.shutdown()

      override def drainAndShutdown[T](streamCompletion: CompletionStage[T], ec: Executor): CompletionStage[T] =
        wrapped.drainAndShutdown(streamCompletion, ec)

      override def isShutdown: CompletionStage[Done] = wrapped.isShutdown

      override def getMetrics: CompletionStage[util.Map[MetricName, Metric]] = wrapped.getMetrics
    }
  }
}

class ControlSpec extends AnyWordSpec with ScalaFutures with Matchers with LogCapturing {
  import ControlSpec._

  val ec = Executors.newCachedThreadPool()

  "Control" should {
    "drain to stream result" in {
      val control = createControl()
      val drainingControl =
        Consumer.createDrainingControl(control, Future.successful("expected").asJava)
      drainingControl.drainAndShutdown(ec).asScala.futureValue should be("expected")
      control.shutdownCalled.get() should be(true)
    }

    "drain to stream failure" in {
      val control = createControl()

      val drainingControl = Consumer.createDrainingControl(
        control,
        Future.failed[String](new RuntimeException("expected")).asJava)
      val value = drainingControl.drainAndShutdown(ec).asScala.failed.futureValue
      value shouldBe a[RuntimeException]
      value.getMessage should be("expected")
      control.shutdownCalled.get() should be(true)
    }

    "drain to stream failure even if shutdown fails" in {
      val control = createControl(shutdownFuture = Future.failed(new RuntimeException("not this")))

      val drainingControl = Consumer.createDrainingControl(
        control,
        Future.failed[String](new RuntimeException("expected")).asJava)
      val value = drainingControl.drainAndShutdown(ec).asScala.failed.futureValue
      value shouldBe a[RuntimeException]
      value.getMessage should be("expected")
      control.shutdownCalled.get() should be(true)
    }

    "drain to shutdown failure when stream succeeds" in {
      val control = createControl(shutdownFuture = Future.failed(new RuntimeException("expected")))

      val drainingControl = Consumer.createDrainingControl(control, Future.successful(Done).asJava)
      val value = drainingControl.drainAndShutdown(ec).asScala.failed.futureValue
      value shouldBe a[RuntimeException]
      value.getMessage should be("expected")
      control.shutdownCalled.get() should be(true)
    }

  }
}
