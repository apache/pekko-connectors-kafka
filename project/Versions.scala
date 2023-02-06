import net.aichler.jupiter.sbt.Import.JupiterKeys
import sbt._
import sbt.Keys._
import sbt.ExclusionRule

object Versions {
  val Nightly: Boolean = sys.env.get("EVENT_NAME").contains("schedule")

  // align ignore-prefixes in scripts/link-validator.conf
  // align in release.yml
  val Scala213 = "2.13.8"

  val pekkoVersionForDocs = "current"
  val pekkoVersion = "0.0.0+26546-767209a8-SNAPSHOT"

  // Keep .scala-steward.conf pin in sync
  val kafkaVersion = "3.0.1"
  val KafkaVersionForDocs = "30"
  // This should align with the ScalaTest version used in the Akka 2.6.x testkit
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
