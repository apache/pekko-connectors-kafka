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

import com.codahale.metrics._
import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko
import pekko.NotUsed
import pekko.kafka.benchmarks.InflightMetrics.{ BrokerMetricRequest, ConsumerMetricRequest }
import pekko.kafka.benchmarks.app.RunTestCommand
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ FileIO, Flow, Sink, Source }
import pekko.util.ByteString

import java.nio.charset.{ Charset, StandardCharsets }
import java.nio.file.Paths
import java.util.concurrent.{ ForkJoinPool, TimeUnit }
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

object Timed extends LazyLogging {
  private val benchmarkReportBasePath = Paths.get("benchmarks", "target")

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool)

  def reporter(metricRegistry: MetricRegistry): ScheduledReporter =
    Slf4jReporter
      .forRegistry(metricRegistry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()

  def csvReporter(metricRegistry: MetricRegistry): ScheduledReporter =
    CsvReporter
      .forRegistry(metricRegistry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build(benchmarkReportBasePath.toFile)

  def inflightMetricsReport(inflight: List[List[String]], testName: String)(
      implicit mat: Materializer) = {
    val metricsReportPath = benchmarkReportBasePath.resolve(Paths.get(s"$testName-inflight-metrics.csv"))
    val metricsReportDetailPath = benchmarkReportBasePath.resolve(Paths.get(s"$testName-inflight-metrics-details.csv"))
    require(inflight.size > 1, "At least 2 records (a header and a data row) are required to make a report.")
    val summary = Source(List(inflight.head, inflight.last))
      .via(format())
      .alsoTo(Sink.foreach(bs => logger.info(bs.utf8String)))
      .runWith(FileIO.toPath(metricsReportPath))
    val details = Source(inflight).via(format()).runWith(FileIO.toPath(metricsReportDetailPath))
    implicit val ec: ExecutionContext = mat.executionContext
    Await.result(Future.sequence(List(summary, details)), 10.seconds)
  }

  private def format[T <: immutable.Iterable[String]](
      delimiter: Char = ',',
      quoteChar: Char = '"',
      escapeChar: Char = '\\',
      endOfLine: String = "\r\n",
      quotingStyle: CsvQuotingStyle = CsvQuotingStyle.Required,
      charset: Charset = StandardCharsets.UTF_8,
      byteOrderMark: Option[ByteString] = None): Flow[T, ByteString, NotUsed] = {
    val formatter =
      new CsvFormatter(delimiter, quoteChar, escapeChar, endOfLine, quotingStyle, charset)
    byteOrderMark.fold {
      Flow[T].map(formatter.toCsv).named("CsvFormatting")
    } { bom =>
      Flow[T].map(formatter.toCsv).named("CsvFormatting").prepend(Source.single(bom))
    }

  }

  def runPerfTest[F](command: RunTestCommand, fixtureGen: FixtureGen[F], testBody: (F, Meter) => Unit): Unit = {
    val name = command.testName
    val msgCount = command.msgCount
    logger.info(s"Generating fixture for $name ${command.filledTopic}")
    val fixture = fixtureGen.generate(msgCount)
    val metrics = new MetricRegistry()
    val meter = metrics.meter(name)
    logger.info(s"Running benchmarks for $name")
    val now = System.nanoTime()
    testBody(fixture, meter)
    val after = System.nanoTime()
    val took = (after - now).nanos
    logger.info(s"Test $name took ${took.toMillis} ms")
    reporter(metrics).report()
    csvReporter(metrics).report()
  }

  def runPerfTestInflightMetrics[F](
      command: RunTestCommand,
      consumerMetricNames: List[ConsumerMetricRequest],
      brokerMetricNames: List[BrokerMetricRequest],
      brokerJmxUrls: List[String],
      fixtureGen: FixtureGen[F],
      testBody: (F, Meter, List[ConsumerMetricRequest], List[BrokerMetricRequest], List[String]) => List[List[String]])(
      implicit mat: Materializer): Unit = {
    val name = command.testName
    val msgCount = command.msgCount
    logger.info(s"Generating fixture for $name ${command.filledTopic}")
    val fixture = fixtureGen.generate(msgCount)
    val metrics = new MetricRegistry()
    val meter = metrics.meter(name)
    logger.info(s"Running benchmarks for $name")
    val now = System.nanoTime()
    val inflight = testBody(fixture, meter, consumerMetricNames, brokerMetricNames, brokerJmxUrls)
    val after = System.nanoTime()
    val took = (after - now).nanos
    logger.info(s"Test $name took ${took.toMillis} ms")
    inflightMetricsReport(inflight, name)
    reporter(metrics).report()
    csvReporter(metrics).report()
  }
}
