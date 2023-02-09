import Versions._
import com.lightbend.paradox.projectinfo.ParadoxProjectInfoPlugin.autoImport.projectInfoVersion
import com.lightbend.sbt.JavaFormatterPlugin.autoImport.javafmtOnCompile
import com.typesafe.tools.mima.plugin.MimaKeys.mimaReportSignatureProblems
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.{ headerLicense, HeaderLicense }
import net.aichler.jupiter.sbt.Import.jupiterTestFramework
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.{ Def, _ }
import sbt.Keys._
import xerial.sbt.Sonatype.autoImport.{ sonatypeCredentialHost, sonatypeProfileName }

object ProjectSettings {
  val onLoadMessage: String = """
    |** Welcome to the Apache Pekko Kafka Connector! **
    |
    |The build has three main modules:
    |  core - the Kafka connector sources
    |  cluster-sharding - Pekko Cluster External Sharding with the Apache Pekko Kafka Connector
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
    |  verifyCodeStyle
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
    organization := "org.apache.pekko",
    organizationName := "Apache Software Foundation",
    organizationHomepage := Some(url("https://www.apache.org")),
    homepage := Some(url("https://pekko.apache.org/docs/pekko-connectors-kafka/current/")),
    scmInfo := Some(ScmInfo(url("https://github.com/apache/incubator-pekko-connectors-kafka"),
      "git@github.com:apache/incubator-pekko-connectors-kafka.git")),
    developers += Developer(
      "pekko-connectors-kafka",
      "Apache Pekko Connectors Kafka Contributors",
      "dev@pekko.apache.org",
      url("https://github.com/apache/incubator-pekko-connectors-kafka/graphs/contributors")),
    startYear := Some(2022),
    licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
    description := "Apache Pekko Kafka Connector is a Reactive Enterprise Integration library for Java and Scala, based on Reactive Streams and Pekko.",
    crossScalaVersions := Seq(Scala213),
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
        val branch = if (isSnapshot.value) "master" else s"v${version.value}"
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
    headerLicense := Some(
      HeaderLicense.Custom(
        """|Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
           |Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
           |""".stripMargin)),
    projectInfoVersion := (if (isSnapshot.value) "snapshot" else version.value),
    publishMavenStyle := true,
    pomIncludeRepository := (_ => false),
    credentials ++= apacheNexusCredentials,
    sonatypeCredentialHost := apacheBaseRepo,
    sonatypeProfileName := "org.apache.pekko")

  private def apacheNexusCredentials: Seq[Credentials] =
    (sys.env.get("NEXUS_USER"), sys.env.get("NEXUS_PW")) match {
      case (Some(user), Some(password)) =>
        Seq(Credentials("Sonatype Nexus Repository Manager", apacheBaseRepo, user, password))
      case _ =>
        Seq.empty
    }
}
