/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt._
import sbt.Keys._
import sbt.ExclusionRule

object Versions {
  val Nightly: Boolean = sys.env.get("EVENT_NAME").contains("schedule")

  // align ignore-prefixes in scripts/link-validator.conf
  val Scala213 = "2.13.16" // update even in link-validator.conf
  val Scala3 = "3.3.6"

  val pekkoVersionForDocs = "current"
  val pekkoConnectorsKafkaVersionForDocs = "current"
  val pekkoManagementVersionForDocs = "current"

  val kafkaVersion = "4.1.0"
  val KafkaVersionForDocs = "37"

  val scalaTestVersion = "3.2.19"
  val scalaPBVersion = "0.11.20"
  val testcontainersVersion = "1.21.3"
  val logbackVersion = "1.5.19"
  val slf4jVersion = "2.0.17"
  // this depends on Kafka, and should be upgraded to such latest version
  // that depends on the same Kafka version, as is defined above
  // See https://mvnrepository.com/artifact/io.confluent/kafka-avro-serializer?repo=confluent-packages
  val confluentAvroSerializerVersion = "8.0.1"
  val confluentLibsExclusionRules = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("com.typesafe.scala-logging"),
    ExclusionRule("org.apache.kafka"))

  val pekkoAPI = "https://pekko.apache.org/api"
  val pekkoJavaAPI = "https://pekko.apache.org/japi"
  val pekkoDocs = "https://pekko.apache.org/docs"

}
