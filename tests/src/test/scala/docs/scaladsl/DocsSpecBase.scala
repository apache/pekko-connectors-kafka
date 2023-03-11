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

package docs.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.kafka.testkit.scaladsl.KafkaSpec
import pekko.kafka.testkit.internal.TestFrameworkInterface
import pekko.stream.scaladsl.Flow
import org.scalatest.Suite
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

abstract class DocsSpecBase(kafkaPort: Int)
    extends KafkaSpec(kafkaPort)
    with AnyFlatSpecLike
    with TestFrameworkInterface.Scalatest
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Eventually {

  this: Suite =>

  protected def this() = this(kafkaPort = -1)

  def businessFlow[T]: Flow[T, T, NotUsed] = Flow[T]

}
