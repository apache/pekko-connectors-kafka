/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import Versions._
import sbt._

object Dependencies {
  lazy val benchmarkDependencies = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "io.dropwizard.metrics" % "metrics-core" % "4.2.33",
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
    "org.testcontainers" % "kafka" % testcontainersVersion % Test,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test)

  lazy val coreDependencies = Seq(
    "org.apache.kafka" % "kafka-clients" % kafkaVersion)

  lazy val testDependencies = Seq(
    "com.google.protobuf" % "protobuf-java" % "4.33.3", // use the same version as in scalapb
    ("io.confluent" % "kafka-avro-serializer" % confluentAvroSerializerVersion % Test).excludeAll(
      confluentLibsExclusionRules: _*),
    "commons-codec" % "commons-codec" % "1.20.0" % Test,
    "jakarta.ws.rs" % "jakarta.ws.rs-api" % "4.0.0" % Test,
    "org.testcontainers" % "kafka" % testcontainersVersion % Test,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "io.spray" %% "spray-json" % "1.3.6" % Test,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.20.1" % Test,
    // See http://hamcrest.org/JavaHamcrest/distributables#upgrading-from-hamcrest-1x
    "org.hamcrest" % "hamcrest-library" % "3.0" % Test,
    "org.hamcrest" % "hamcrest" % "3.0" % Test,
    "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion % Test,
    // Schema registry uses Glassfish which uses java.util.logging
    "org.slf4j" % "jul-to-slf4j" % slf4jVersion % Test,
    "org.mockito" % "mockito-core" % "5.21.0" % Test,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalaPBVersion % Test)

  lazy val testKitDependencies = Seq(
    "org.testcontainers" % "kafka" % testcontainersVersion % Provided,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Provided,
    "junit" % "junit" % "4.13.2" % Provided)

}
