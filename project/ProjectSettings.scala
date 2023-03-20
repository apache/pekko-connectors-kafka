/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

import Versions._
import com.lightbend.paradox.projectinfo.ParadoxProjectInfoPlugin.autoImport.projectInfoVersion
import com.lightbend.sbt.JavaFormatterPlugin.autoImport.javafmtOnCompile
import com.typesafe.tools.mima.plugin.MimaKeys.mimaReportSignatureProblems
import net.aichler.jupiter.sbt.Import.jupiterTestFramework
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.{ Def, _ }
import sbt.Keys._
import org.mdedetrich.apache.sonatype.SonatypeApachePlugin
import SonatypeApachePlugin.autoImport.apacheSonatypeDisclaimerFile
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverSonatypeSnapshots

object ProjectSettings extends AutoPlugin {

  override val requires = SonatypeApachePlugin && DynVerPlugin

  override def trigger = allRequirements

  val onLoadMessage: String = """
    |** Welcome to the Apache Pekko Kafka Connector! **
    |
    |The build has three main modules:
    |  core - the Kafka connector sources
    |  cluster-sharding - Apache Pekko Cluster External Sharding with the Apache Pekko Kafka Connector
    |  tests - tests, Docker based integration tests, code for the documentation
    |  testkit - framework for testing the connector
    |
    |Other modules:
    |  docs - the sources for generating https://pekko.apache.org/docs/pekko-connectors-kafka/current/
    |  benchmarks - compare direct Kafka API usage with Apache Pekko Kafka Connector
    |
    |Useful sbt tasks:
    |
    |  docs/previewSite
    |    builds Paradox and Scaladoc documentation, starts a webserver and
    |    opens a new browser window
    |
    |  verifyCodeFormat
    |    checks if all of the code is formatted according to the configuration
    |
    |  verifyDocs
    |    builds all of the docs
    |
    |  test
    |    runs all the tests
    |
    |  tests/IntegrationTest/test
    |    run integration tests backed by Docker containers
    |
    |  tests/testOnly -- -t "A consume-transform-produce cycle must complete in happy-path scenario"
    |    run a single test with an exact name (use -z for partial match)
    |
    |  benchmarks/IntegrationTest/testOnly *.PekkoConnectorsKafkaPlainConsumer
    |    run a single benchmark backed by Docker containers
          """.stripMargin

  private val apacheBaseRepo = "repository.apache.org"

  lazy val commonSettings: Seq[Def.Setting[_]] = Def.settings(
    homepage := Some(url("https://pekko.apache.org/docs/pekko-connectors-kafka/current/")),
    scmInfo := Some(ScmInfo(url("https://github.com/apache/incubator-pekko-connectors-kafka"),
      "git@github.com:apache/incubator-pekko-connectors-kafka.git")),
    developers += Developer(
      "pekko-connectors-kafka",
      "Apache Pekko Connectors Kafka Contributors",
      "dev@pekko.apache.org",
      url("https://github.com/apache/incubator-pekko-connectors-kafka/graphs/contributors")),
    startYear := Some(2022),
    description := "Apache Pekko Kafka Connector is a Reactive Enterprise Integration library for Java and Scala, based on Reactive Streams and Apache Pekko.",
    crossScalaVersions := Seq(Scala212, Scala213),
    scalaVersion := Scala213,
    crossVersion := CrossVersion.binary,
    javacOptions ++= Seq(
      "-Xlint:deprecation",
      "-Xlint:unchecked"),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-Wconf:cat=feature:w,cat=deprecation:w,cat=unchecked:w,cat=lint:w,cat=unused:w,cat=w-flag:w") ++ {
      if (insideCI.value && !Nightly) Seq("-Werror")
      else Seq.empty
    },
    Compile / doc / scalacOptions := scalacOptions.value ++ Seq(
      "-Wconf:cat=scaladoc:i",
      "-doc-title",
      "Apache Pekko Kafka Connector",
      "-doc-version",
      version.value,
      "-sourcepath",
      (ThisBuild / baseDirectory).value.toString,
      "-skip-packages",
      "pekko.pattern:scala", // for some reason Scaladoc creates this
      "-doc-source-url", {
        val branch = if (isSnapshot.value) "main" else s"v${version.value}"
        s"https://github.com/apache/incubator-pekko-connectors-kafka/tree/${branch}€{FILE_PATH_EXT}#L€{FILE_LINE}"
      },
      "-doc-canonical-base-url",
      "https://pekko.apache.org/api/pekko-connectors-kafka/current/"),
    Compile / doc / scalacOptions -= "-Xfatal-warnings",
    // show full stack traces and test case durations
    testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
    // https://github.com/maichler/sbt-jupiter-interface#framework-options
    // -a Show stack traces and exception class name for AssertionErrors.
    // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
    // -q Suppress stdout for successful tests.
    // -s Try to decode Scala names in stack traces and test names.
    testOptions += Tests.Argument(jupiterTestFramework, "-a", "-v", "-q", "-s"),
    scalafmtOnCompile := false,
    javafmtOnCompile := false,
    ThisBuild / mimaReportSignatureProblems := true,
    projectInfoVersion := (if (isSnapshot.value) "snapshot" else version.value),
    apacheSonatypeDisclaimerFile := Some((LocalRootProject / baseDirectory).value / "DISCLAIMER"))

  override lazy val buildSettings = Seq(
    dynverSonatypeSnapshots := true)
}
