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

private[pekko] object CommitTrigger {
  sealed trait TriggerdBy
  case object BatchSize extends TriggerdBy {
    override def toString: String = "batch size"
  }
  case object Interval extends TriggerdBy {
    override def toString: String = "interval"
  }
  case object UpstreamClosed extends TriggerdBy {
    override def toString: String = "upstream closed"
  }
  case object UpstreamFinish extends TriggerdBy {
    override def toString: String = "upstream finish"
  }
  case object UpstreamFailure extends TriggerdBy {
    override def toString: String = "upstream failure"
  }
}
