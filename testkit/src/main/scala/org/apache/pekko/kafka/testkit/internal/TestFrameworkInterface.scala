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

package org.apache.pekko.kafka.testkit.internal

import org.scalatest.{ BeforeAndAfterAll, Suite }

trait TestFrameworkInterface {
  def setUp(): Unit
  def cleanUp(): Unit
}

object TestFrameworkInterface {

  trait Scalatest extends TestFrameworkInterface with BeforeAndAfterAll {
    this: Suite =>

    abstract override protected def beforeAll(): Unit = {
      setUp()
      super.beforeAll()
    }

    abstract override protected def afterAll(): Unit = {
      cleanUp()
      super.afterAll()
    }
  }
}
