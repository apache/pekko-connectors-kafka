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

import org.apache.pekko.actor.{ Actor, ActorLogging }
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.stream.stage.{ GraphStageLogic, StageLogging }

/**
 * Generate a short random UID for something.
 */
private[internal] trait InstanceId {
  private val instanceId = java.util.UUID.randomUUID().toString.take(5)
  def id: String = instanceId
}

/**
 * Override Apache Pekko streams [[StageLogging]] to include an ID from [[InstanceId]] as a prefix to each logging statement.
 */
private[internal] trait StageIdLogging extends StageLogging with InstanceId { self: GraphStageLogic =>
  private[this] var _log: LoggingAdapter = _
  protected def idLogPrefix: String = s"[$id] "
  override def log: LoggingAdapter = {
    if (_log eq null) {
      _log = new LoggingAdapterWithPrefix(super.log, idLogPrefix)
    }
    _log
  }
}

/**
 * Override Apache Pekko classic [[ActorLogging]] to include an ID from [[InstanceId]] as a prefix to each logging statement.
 */
private[internal] trait ActorIdLogging extends ActorLogging with InstanceId { this: Actor =>
  private[this] var _log: LoggingAdapter = _
  protected def idLogPrefix: String = s"[$id] "
  override def log: LoggingAdapter = {
    if (_log eq null) {
      _log = new LoggingAdapterWithPrefix(super.log, idLogPrefix)
    }
    _log
  }
}

private[internal] final class LoggingAdapterWithPrefix(logger: LoggingAdapter, prefix: String) extends LoggingAdapter {
  private def msgWithId(message: String): String = prefix + message

  override protected def notifyError(message: String): Unit = logger.error(msgWithId(message))
  override protected def notifyError(cause: Throwable, message: String): Unit = logger.error(cause, msgWithId(message))
  override protected def notifyWarning(message: String): Unit = logger.warning(msgWithId(message))
  override protected def notifyInfo(message: String): Unit = logger.info(msgWithId(message))
  override protected def notifyDebug(message: String): Unit = logger.debug(msgWithId(message))

  override def isErrorEnabled: Boolean = logger.isErrorEnabled
  override def isWarningEnabled: Boolean = logger.isWarningEnabled
  override def isInfoEnabled: Boolean = logger.isInfoEnabled
  override def isDebugEnabled: Boolean = logger.isDebugEnabled
}
