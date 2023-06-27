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
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.{ paradoxGroups, paradoxProperties, paradoxRoots }
import org.apache.pekko.PekkoParadoxPlugin.autoImport._
import sbt._
import sbt.Keys._

object ParadoxSettings {

  val themeSettings = Seq(
    // allow access to snapshots for pekko-sbt-paradox
    resolvers += "Apache Nexus Snapshots".at("https://repository.apache.org/content/repositories/snapshots/"),
    pekkoParadoxGithub := Some("https://github.com/apache/incubator-pekko-connectors-kafka"))

  val propertiesSettings = Seq(
    apidocRootPackage := "org.apache.pekko",
    paradoxGroups := Map("Language" -> Seq("Java", "Scala")),
    paradoxRoots := List("index.html"),
    Compile / paradoxProperties ++= Map(
      "image.base_url" -> "images/",
      "confluent.version" -> confluentAvroSerializerVersion,
      "scalatest.version" -> scalaTestVersion,
      "pekko.version" -> pekkoVersion,
      "extref.pekko.base_url" -> s"$pekkoDocs/pekko/$pekkoVersionForDocs/%s",
      "scaladoc.org.apache.pekko.base_url" -> s"$pekkoAPI/pekko/$pekkoVersionForDocs/",
      "javadoc.org.apache.pekko.base_url" -> s"$pekkoAPI/pekko/$pekkoVersionForDocs/",
      "javadoc.pekko.link_style" -> "direct",
      "extref.pekko-management.base_url" -> s"$pekkoDocs/pekko-management/$pekkoVersionForDocs/%s",
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

  val settings = propertiesSettings ++ themeSettings
}
