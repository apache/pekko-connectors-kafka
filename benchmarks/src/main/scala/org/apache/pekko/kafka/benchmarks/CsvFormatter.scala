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

package org.apache.pekko.kafka.benchmarks

import org.apache.pekko.util.ByteString

import java.nio.charset.{ Charset, StandardCharsets }
import scala.collection.immutable

private[benchmarks] sealed trait CsvQuotingStyle

object CsvQuotingStyle {

  /** Quote only fields requiring quotes */
  case object Required extends CsvQuotingStyle

  /** Quote all fields */
  case object Always extends CsvQuotingStyle
}

// TODO: This needs to be deleted after migrating alpakka to pekko.
// This is just temporary base to see everything compiles and tests will pass without any issue
private[benchmarks] class CsvFormatter(delimiter: Char,
    quoteChar: Char,
    escapeChar: Char,
    endOfLine: String,
    quotingStyle: CsvQuotingStyle,
    charset: Charset = StandardCharsets.UTF_8) {

  private[this] val charsetName = charset.name()

  private[this] val delimiterBs = ByteString(String.valueOf(delimiter), charsetName)
  private[this] val quoteBs = ByteString(String.valueOf(quoteChar), charsetName)
  private[this] val duplicatedQuote = ByteString(String.valueOf(Array(quoteChar, quoteChar)), charsetName)
  private[this] val duplicatedEscape = ByteString(String.valueOf(Array(escapeChar, escapeChar)), charsetName)
  private[this] val endOfLineBs = ByteString(endOfLine, charsetName)

  def toCsv(fields: immutable.Iterable[Any]): ByteString =
    if (fields.nonEmpty) nonEmptyToCsv(fields)
    else endOfLineBs

  private def nonEmptyToCsv(fields: immutable.Iterable[Any]) = {
    val builder = ByteString.createBuilder

    def splitAndDuplicateQuotesAndEscapes(field: String, splitAt: Int) = {

      @inline def indexOfQuoteOrEscape(lastIndex: Int) = {
        var index = lastIndex
        var found = -1
        while (index < field.length && found == -1) {
          val char = field(index)
          if (char == quoteChar || char == escapeChar) found = index
          index += 1
        }
        found
      }

      var lastIndex = 0
      var index = splitAt
      while (index > -1) {
        builder ++= ByteString.apply(field.substring(lastIndex, index), charsetName)
        val char = field.charAt(index)
        if (char == quoteChar) {
          builder ++= duplicatedQuote
        } else {
          builder ++= duplicatedEscape
        }
        lastIndex = index + 1
        index = indexOfQuoteOrEscape(lastIndex)
      }
      if (lastIndex < field.length) {
        builder ++= ByteString(field.substring(lastIndex), charsetName)
      }
    }

    def append(field: String) = {
      val (quoteIt, splitAt) = requiresQuotesOrSplit(field)
      if (quoteIt || quotingStyle == CsvQuotingStyle.Always) {
        builder ++= quoteBs
        if (splitAt != -1) {
          splitAndDuplicateQuotesAndEscapes(field, splitAt)
        } else {
          builder ++= ByteString(field, charsetName)
        }
        builder ++= quoteBs
      } else {
        builder ++= ByteString(field, charsetName)
      }
    }

    val iterator = fields.iterator
    var hasNext = iterator.hasNext
    while (hasNext) {
      val next = iterator.next()
      if (next != null) {
        append(next.toString)
      }
      hasNext = iterator.hasNext
      if (hasNext) {
        builder ++= delimiterBs
      }
    }
    builder ++= endOfLineBs
    builder.result()
  }

  private def requiresQuotesOrSplit(field: String): (Boolean, Int) = {
    var quotes = CsvQuotingStyle.Always == quotingStyle
    var split = -1
    var index = 0
    while (index < field.length && !(quotes && split != -1)) {
      val char = field(index)
      if (char == `quoteChar` || char == `escapeChar`) {
        quotes = true
        split = index
      } else if (char == '\r' || char == '\n' || char == `delimiter`) {
        quotes = true
      }
      index += 1
    }
    (quotes, split)
  }
}
