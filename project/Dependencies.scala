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
  lazy val benchmarkDependencies = Def.setting(Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "io.dropwizard.metrics" % "metrics-core" % "4.2.25",
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
    "org.testcontainers" % "kafka" % testcontainersVersion % IntegrationTest,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion % IntegrationTest,
    "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % IntegrationTest,
    "org.scalatest" %% "scalatest" % scalaTestVersion % IntegrationTest))

  lazy val clusterShardingDependencies = Seq("org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion)

  lazy val coreDependencies = Seq(
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
    "org.apache.pekko" %% "pekko-discovery" % pekkoVersion % Provided,
    "org.apache.kafka" % "kafka-clients" % kafkaVersion)

  lazy val testDependencies = Def.setting(Seq(
    "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
    "com.google.protobuf" % "protobuf-java" % "3.25.3", // use the same version as in scalapb
    ("io.confluent" % "kafka-avro-serializer" % confluentAvroSerializerVersion % Test).excludeAll(
      confluentLibsExclusionRules: _*),
    "jakarta.ws.rs" % "jakarta.ws.rs-api" % "3.1.0" % Test,
    "org.testcontainers" % "kafka" % testcontainersVersion % Test,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "io.spray" %% "spray-json" % "1.3.6" % Test,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3" % Test,
    // See http://hamcrest.org/JavaHamcrest/distributables#upgrading-from-hamcrest-1x
    "org.hamcrest" % "hamcrest-library" % "2.2" % Test,
    "org.hamcrest" % "hamcrest" % "2.2" % Test,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion % Test,
    "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion % Test,
    // Schema registry uses Glassfish which uses java.util.logging
    "org.slf4j" % "jul-to-slf4j" % slf4jVersion % Test,
    "org.mockito" % "mockito-core" % "4.11.0" % Test,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalaPBVersion % Test))

  lazy val testKitDependencies = Def.setting(Seq(
    "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion,
    "org.testcontainers" % "kafka" % testcontainersVersion % Provided,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Provided,
    "junit" % "junit" % "4.13.2" % Provided))

}
