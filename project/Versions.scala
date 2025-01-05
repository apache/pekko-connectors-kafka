/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import net.aichler.jupiter.sbt.Import.JupiterKeys
import sbt._
import sbt.Keys._
import sbt.ExclusionRule

object Versions {
  val Nightly: Boolean = sys.env.get("EVENT_NAME").contains("schedule")

  // align ignore-prefixes in scripts/link-validator.conf
  val Scala213 = "2.13.15" // update even in link-validator.conf
  val Scala212 = "2.12.20"
  val Scala3 = "3.3.4"

  val pekkoVersionForDocs = "current"
  val pekkoConnectorsKafkaVersionForDocs = "current"
  val pekkoManagementVersionForDocs = "current"

  val kafkaVersion = "3.9.0"
  val KafkaVersionForDocs = "37"

  val scalaTestVersion = "3.2.19"
  val scalaPBVersion = "0.11.17"
  val testcontainersVersion = "1.20.4"
  val logbackVersion = "1.3.15"
  val slf4jVersion = "2.0.16"
  // this depends on Kafka, and should be upgraded to such latest version
  // that depends on the same Kafka version, as is defined above
  // See https://mvnrepository.com/artifact/io.confluent/kafka-avro-serializer?repo=confluent-packages
  val confluentAvroSerializerVersion = "7.8.0"
  val confluentLibsExclusionRules = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("com.typesafe.scala-logging"),
    ExclusionRule("org.apache.kafka"))

  val pekkoAPI = "https://pekko.apache.org/api"
  val pekkoJavaAPI = "https://pekko.apache.org/japi"
  val pekkoDocs = "https://pekko.apache.org/docs"

}
