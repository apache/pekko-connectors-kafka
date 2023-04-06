/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

import net.aichler.jupiter.sbt.Import.JupiterKeys
import sbt._
import sbt.Keys._
import sbt.ExclusionRule

object Versions {
  val Nightly: Boolean = sys.env.get("EVENT_NAME").contains("schedule")

  // align ignore-prefixes in scripts/link-validator.conf
  val Scala213 = "2.13.10" // update even in link-validator.conf
  val Scala212 = "2.12.17"

  val pekkoVersionForDocs = "current"
  val pekkoVersion = "0.0.0+26630-2c4d0ee0-SNAPSHOT"

  // Keep .scala-steward.conf pin in sync
  val kafkaVersion = "3.0.1"
  val KafkaVersionForDocs = "30"
  // This should align with the ScalaTest version used in the Apache Pekko 1.0.x testkit
  // https://github.com/apache/incubator-pekko/blob/main/project/Dependencies.scala#L70
  val scalaTestVersion = "3.1.4"
  val testcontainersVersion = "1.16.3"
  val slf4jVersion = "1.7.36"
  // this depends on Kafka, and should be upgraded to such latest version
  // that depends on the same Kafka version, as is defined above
  // See https://mvnrepository.com/artifact/io.confluent/kafka-avro-serializer?repo=confluent-packages
  val confluentAvroSerializerVersion = "7.0.5"
  val confluentLibsExclusionRules = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("com.typesafe.scala-logging"),
    ExclusionRule("org.apache.kafka"))

  val pekkoAPI = "https://pekko.apache.org/api"
  val pekkoDocs = "https://pekko.apache.org/docs"

}
