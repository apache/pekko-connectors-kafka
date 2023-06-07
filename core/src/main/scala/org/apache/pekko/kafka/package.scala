/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko

package object kafka {
  private[kafka] def convertPropertiesToSafeText(properties: Map[String, String]): String = {
    properties.toSeq
      .map {
        case (key, value) if value.isEmpty =>
          key -> ""
        case (key, _) =>
          key -> "*****"
      }
      .sortBy(_._1)
      .mkString(",")
  }
}
