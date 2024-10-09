/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import Versions._
import com.lightbend.paradox.apidoc.ApidocPlugin.autoImport.apidocRootPackage
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.{
  paradox,
  paradoxGroups,
  paradoxMarkdownToHtml,
  paradoxProperties,
  paradoxRoots
}
import org.apache.pekko.PekkoParadoxPlugin.autoImport._
import sbt._
import sbt.Keys._
import sbtlicensereport.SbtLicenseReport.autoImportImpl.dumpLicenseReportAggregate

object ParadoxSettings {

  val themeSettings = Seq(
    pekkoParadoxGithub := Some("https://github.com/apache/pekko-connectors-kafka"))

  val propertiesSettings = Seq(
    apidocRootPackage := "org.apache.pekko",
    paradoxGroups := Map("Language" -> Seq("Java", "Scala")),
    paradoxRoots := List("index.html"),
    Compile / paradoxProperties ++= Map(
      "image.base_url" -> "images/",
      "confluent.version" -> confluentAvroSerializerVersion,
      "scalatest.version" -> scalaTestVersion,
      "pekko.version" -> PekkoCoreDependency.version,
      "extref.pekko.base_url" -> s"$pekkoDocs/pekko/$pekkoVersionForDocs/%s",
      "scaladoc.org.apache.pekko.base_url" -> s"$pekkoAPI/pekko/$pekkoVersionForDocs/",
      "javadoc.org.apache.pekko.base_url" -> s"$pekkoAPI/pekko/$pekkoVersionForDocs/",
      "scaladoc.org.apache.pekko.kafka.base_url" -> s"$pekkoAPI/pekko-connectors-kafka/$pekkoConnectorsKafkaVersionForDocs/",
      "javadoc.org.apache.pekko.kafka.base_url" -> s"$pekkoAPI/pekko-connectors-kafka/$pekkoConnectorsKafkaVersionForDocs/",
      "javadoc.org.apache.pekko.link_style" -> "direct",
      "extref.pekko-management.base_url" -> s"$pekkoDocs/pekko-management/$pekkoManagementVersionForDocs/%s",
      // Kafka
      "kafka.version" -> kafkaVersion,
      "extref.kafka.base_url" -> s"https://kafka.apache.org/$KafkaVersionForDocs/%s",
      "javadoc.org.apache.kafka.base_url" -> s"https://kafka.apache.org/$KafkaVersionForDocs/javadoc/",
      "javadoc.org.apache.kafka.link_style" -> "direct",
      // Java
      "extref.java-docs.base_url" -> "https://docs.oracle.com/en/java/javase/11/%s",
      "javadoc.base_url" -> "https://docs.oracle.com/en/java/javase/11/docs/api/java.base/",
      "javadoc.link_style" -> "direct",
      // Scala
      "scaladoc.scala.base_url" -> s"https://www.scala-lang.org/api/current/",
      "scaladoc.com.typesafe.config.base_url" -> s"https://lightbend.github.io/config/latest/api/",
      // Testcontainers
      "testcontainers.version" -> testcontainersVersion,
      "javadoc.org.testcontainers.containers.base_url" -> s"https://www.javadoc.io/doc/org.testcontainers/testcontainers/$testcontainersVersion/",
      "javadoc.org.testcontainers.containers.link_style" -> "direct"))

  val sourceGeneratorSettings = Seq(
    Compile / paradoxMarkdownToHtml / sourceGenerators += Def.taskDyn {
      val targetFile = (Compile / paradox / sourceManaged).value / "license-report.md"

      (LocalRootProject / dumpLicenseReportAggregate).map { dir =>
        IO.copy(List(dir / "pekko-connectors-kafka-root-licenses.md" -> targetFile)).toList
      }
    }.taskValue)

  val settings = propertiesSettings ++ themeSettings ++ sourceGeneratorSettings
}
