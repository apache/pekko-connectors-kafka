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

package org.apache.pekko.kafka.tests

import org.apache.pekko.annotation.InternalApi
import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory

/**
 * See https://pekko.apache.org/docs/pekko/current/typed/testing-async.html#silence-logging-output-from-tests
 *
 * INTERNAL API
 */
@InternalApi private[pekko] object LogbackUtil {
  def loggerNameOrRoot(loggerName: String): String =
    if (loggerName == "") org.slf4j.Logger.ROOT_LOGGER_NAME else loggerName

  def getLogbackLogger(loggerName: String): ch.qos.logback.classic.Logger = {
    LoggerFactory.getLogger(loggerNameOrRoot(loggerName)) match {
      case logger: ch.qos.logback.classic.Logger => logger
      case null                                  =>
        throw new IllegalArgumentException(s"Couldn't find logger for [$loggerName].")
      case other =>
        throw new IllegalArgumentException(
          s"Requires Logback logger for [$loggerName], it was a [${other.getClass.getName}]")
    }
  }

  def convertLevel(level: ch.qos.logback.classic.Level): Level = {
    level.levelInt match {
      case ch.qos.logback.classic.Level.TRACE_INT => Level.TRACE
      case ch.qos.logback.classic.Level.DEBUG_INT => Level.DEBUG
      case ch.qos.logback.classic.Level.INFO_INT  => Level.INFO
      case ch.qos.logback.classic.Level.WARN_INT  => Level.WARN
      case ch.qos.logback.classic.Level.ERROR_INT => Level.ERROR
      case _                                      =>
        throw new IllegalArgumentException("Level " + level.levelStr + ", " + level.levelInt + " is unknown.")
    }
  }
}
