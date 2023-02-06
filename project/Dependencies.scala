import Versions._
import sbt._

object Dependencies {
  lazy val benchmarkDependencies = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "io.dropwizard.metrics" % "metrics-core" % "4.2.11",
    "ch.qos.logback" % "logback-classic" % "1.2.11",
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
    "org.testcontainers" % "kafka" % testcontainersVersion % IntegrationTest,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion % IntegrationTest,
    "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % IntegrationTest,
    "org.scalatest" %% "scalatest" % scalaTestVersion % IntegrationTest)

  lazy val clusterShardingDependencies = Seq("org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion)

  lazy val coreDependencies = Seq(
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
    "org.apache.pekko" %% "pekko-discovery" % pekkoVersion % Provided,
    "org.apache.kafka" % "kafka-clients" % kafkaVersion)

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
    "com.google.protobuf" % "protobuf-java" % "3.19.1", // use the same version as in scalapb
    ("io.confluent" % "kafka-avro-serializer" % confluentAvroSerializerVersion % Test).excludeAll(
      confluentLibsExclusionRules: _*),
    // See https://github.com/sbt/sbt/issues/3618#issuecomment-448951808
    ("javax.ws.rs" % "javax.ws.rs-api" % "2.1.1").artifacts(Artifact("javax.ws.rs-api", "jar", "jar")),
    "org.testcontainers" % "kafka" % testcontainersVersion % Test,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "io.spray" %% "spray-json" % "1.3.6" % Test,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3" % Test, // ApacheV2
    // See http://hamcrest.org/JavaHamcrest/distributables#upgrading-from-hamcrest-1x
    "org.hamcrest" % "hamcrest-library" % "2.2" % Test,
    "org.hamcrest" % "hamcrest" % "2.2" % Test,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.11" % Test,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion % Test,
    // Schema registry uses Glassfish which uses java.util.logging
    "org.slf4j" % "jul-to-slf4j" % slf4jVersion % Test,
    "org.mockito" % "mockito-core" % "4.6.1" % Test,
    "com.thesamet.scalapb" %% "scalapb-runtime" % "0.10.11" % Test)

  lazy val testKitDependencies = Seq(
    "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion,
    "org.testcontainers" % "kafka" % testcontainersVersion % Provided,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Provided,
    "junit" % "junit" % "4.13.2" % Provided)

}
