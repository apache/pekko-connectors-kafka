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

package org.apache.pekko.kafka

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Flow
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.testcontainers.containers.GenericContainer

import scala.jdk.CollectionConverters._

object IntegrationTests {
  val MessageLogInterval = 500L

  def logSentMessages()(implicit log: Logger): Flow[Long, Long, NotUsed] = Flow[Long].map { i =>
    if (i % MessageLogInterval == 0) log.info(s"Sent [$i] messages so far.")
    i
  }

  def logReceivedMessages()(implicit log: Logger): Flow[Long, Long, NotUsed] = Flow[Long].map { i =>
    if (i % MessageLogInterval == 0) log.info(s"Received [$i] messages so far.")
    i
  }

  def logReceivedMessages(tp: TopicPartition)(implicit log: Logger): Flow[Long, Long, NotUsed] = Flow[Long].map { i =>
    if (i % MessageLogInterval == 0) log.info(s"$tp: Received [$i] messages so far.")
    i
  }

  def stopRandomBroker(brokers: Vector[GenericContainer[_]], msgCount: Long)(implicit log: Logger): Unit = {
    val broker: GenericContainer[_] = brokers(scala.util.Random.nextInt(brokers.length))
    val id = broker.getContainerId
    val networkAliases = broker.getNetworkAliases.asScala.mkString(",")
    log.warn(
      s"Stopping one Kafka container with network aliases [$networkAliases], container id [$id], after [$msgCount] messages")
    broker.stop()
  }

}
