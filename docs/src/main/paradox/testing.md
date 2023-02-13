---
project.description: Apache Pekko Connectors Kafka provides a Testkit with support for running local Kafka brokers for integration tests.
---
# Testing

To simplify testing of streaming integrations with Apache Pekko Connectors Kafka, it provides the **Apache Pekko Connectors Kafka testkit**. It provides help for

* @ref:[Using Docker to launch a local Kafka cluster with testcontainers](testing-testcontainers.md)
* @ref:[Mocking the Apache Pekko Connectors Kafka Consumers and Producers](#mocking-the-consumer-or-producer)

@@project-info{ projectId="testkit" }

@@dependency [Maven,sbt,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-kafka-testkit_$scala.binary.version$
  version=$project.version$
  scope=test
  symbol2=PekkoVersion
  value2="$pekko.version$"
  group2=org.apache.pekko
  artifact2=pekko-stream-testkit_$scala.binary.version$
  version2=PekkoVersion
  scope2=test
}

Note that Apache Pekko testkits do not promise binary compatibility. The API might be changed even between patch releases.

The table below shows Apache Pekko Connectors Kafka testkit's direct dependencies and the second tab shows all libraries it depends on transitively. 

@@dependencies { projectId="testkit" }

## Running Kafka with your tests

The Testkit provides a variety of ways to test your application against a real Kafka broker or cluster using @ref:[Testcontainers (Docker)](testing-testcontainers.md).

The table below helps guide you to the right Testkit implementation depending on your programming language, testing framework, and use (or not) of Docker containers.
You must mix in or implement these types into your test classes to use them.
See the documentation for each for more details.

| Type                                                                                                                                                    | Test Framework     | Cluster     | Lang         | Lifetime                 |
|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|-------------|--------------|--------------------------|
| @ref:[`org.apache.pekko.kafka.testkit.javadsl.TestcontainersKafkaJunit4Test`](testing-testcontainers.md#testing-with-a-docker-kafka-cluster-from-java-code)         | JUnit 4            | Yes         | Java         | All tests, Per class     |
| @ref:[`org.apache.pekko.kafka.testkit.javadsl.TestcontainersKafkaTest`](testing-testcontainers.md#testing-with-a-docker-kafka-cluster-from-java-code)               | JUnit 5            | Yes         | Java         | All tests, Per class     |
| @ref:[`org.apache.pekko.kafka.testkit.scaladsl.TestcontainersKafkaLike`](testing-testcontainers.md#testing-with-a-docker-kafka-cluster-from-scala-code)             | ScalaTest          | Yes         | Scala        | All tests                |
| @ref:[`org.apache.pekko.kafka.testkit.scaladsl.TestcontainersKafkaPerClassLike`](testing-testcontainers.md#testing-with-a-docker-kafka-cluster-from-scala-code)     | ScalaTest          | Yes         | Scala        | Per class                |

## Alternative testing libraries

If using Maven and Java, an alternative library that provides running Kafka broker instance during testing is [kafka-unit by salesforce](https://github.com/salesforce/kafka-junit). It has support for Junit 4 and 5 and supports many different versions of Kafka.

## Mocking the Consumer or Producer

The testkit contains factories to create the messages emitted by Consumer sources in `org.apache.pekko.kafka.testkit.ConsumerResultFactory` and Producer flows in `org.apache.pekko.kafka.testkit.ProducerResultFactory`.

To create the materialized value of Consumer sources, @apidoc[ConsumerControlFactory$] offers a wrapped @apidoc[KillSwitch].

Scala
: @@snip [snip](/tests/src/test/scala/docs/scaladsl/TestkitSamplesSpec.scala) { #factories }

Java
: @@snip [snip](/tests/src/test/java/docs/javadsl/TestkitSamplesTest.java) { #factories }

@@@ index

* [testcontainers](testing-testcontainers.md)

@@@
