# Overview

The [Apache Pekko Kafka Connector project](https://pekko.apache.org/docs/pekko-connectors-kafka/current/) is an open source initiative to implement stream-aware and reactive integration pipelines for Java and Scala. It is built on top of @extref[Pekko Streams](pekko:stream/index.html), and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Apache Pekko Streams is a [Reactive Streams](https://www.reactive-streams.org/) and JDK 9+ @extref[java.util.concurrent.Flow](java-docs:docs/api/java.base/java/util/concurrent/Flow.html)-compliant implementation and therefore @extref[fully interoperable](pekko:general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

This **Apache Pekko Connectors Kafka connector** lets you connect [Apache Kafka](https://kafka.apache.org/) to Apache Pekko Streams. It was formerly known as **Apache Pekko Streams Kafka** and even **Reactive Kafka**.

## Project Info

@@project-info{ projectId="core" }

## Matching Kafka Versions

|Kafka client | Scala Versions | Apache Pekko version | Apache Pekko Connectors Kafka Connector
|-------------|----------------|--------------|-------------------------
|[3.0.1](https://dist.apache.org/repos/dist/release/kafka/3.0.1/RELEASE_NOTES.html) | 2.13             | 2.6.18+         | [release 3.0.1](https://github.com/akka/alpakka-kafka/releases/tag/v3.0.0)
|[3.0.0](https://blogs.apache.org/kafka/entry/what-s-new-in-apache6)                | 2.13             | 2.6.18+         | [release 3.0.0 RC1](https://github.com/akka/alpakka-kafka/releases/tag/v3.0.0-RC1)
|[2.7.0](https://archive.apache.org/dist/kafka/2.7.0/RELEASE_NOTES.html) | 2.13, 2.12       | 2.6.14+         | @ref:[release 2.1.0](release-notes/2.1.x.md)
|[2.4.1](https://archive.apache.org/dist/kafka/2.4.1/RELEASE_NOTES.html) | 2.13, 2.12, 2.11 | 2.5.31+, 2.6.6+ | @ref:[release 2.0.5](release-notes/2.0.x.md)
|[2.4.1](https://archive.apache.org/dist/kafka/2.4.1/RELEASE_NOTES.html) | 2.13, 2.12, 2.11 | 2.5.30+, 2.6.6+ | @ref:[release 2.0.4](release-notes/2.0.x.md)
|[2.4.1](https://archive.apache.org/dist/kafka/2.4.1/RELEASE_NOTES.html) | 2.13, 2.12, 2.11 | 2.5.30+, 2.6.3+ | @ref:[release 2.0.3](release-notes/2.0.x.md)
|[2.4.0](https://archive.apache.org/dist/kafka/2.4.0/RELEASE_NOTES.html) | 2.13, 2.12, 2.11 | 2.5.23+, 2.6.x | @ref:[release 2.0.0](release-notes/2.0.x.md)
|[2.1.1](https://archive.apache.org/dist/kafka/2.1.1/RELEASE_NOTES.html) | 2.13, 2.12, 2.11 | 2.5.x        | @ref:[release 1.0.4](release-notes/1.0.x.md#1-0-4)
|[2.1.1](https://archive.apache.org/dist/kafka/2.1.1/RELEASE_NOTES.html) | 2.12, 2.11       | 2.5.x        | @ref:[release 1.0.1](release-notes/1.0.x.md#1-0-1)
|[2.1.0](https://archive.apache.org/dist/kafka/2.1.0/RELEASE_NOTES.html) | 2.12, 2.11       | 2.5.x        | @ref:[release 1.0](release-notes/1.0.x.md#1-0)
|1.1.x        | 2.12, 2.11 | 2.5.x        | [release 0.20+](https://github.com/akka/alpakka-kafka/releases)
|1.0.x        | 2.12, 2.11 | 2.5.x        | [release 0.20+](https://github.com/akka/alpakka-kafka/releases)
|0.11.x       | 2.12, 2.11 | 2.5.x        | [release 0.19](https://github.com/akka/alpakka-kafka/milestone/19?closed=1)

@@@ note

As Kafka's client protocol negotiates the version to use with the Kafka broker, you may use a Kafka client version that is different than the Kafka broker's version.

These clients can communicate with brokers that are version 2.1.0 or newer. Older or newer brokers may not support certain features. You will receive an UnsupportedVersionException when invoking an API that is not available on the running broker version.

Check even Confluent's [Versions and Interoperability](https://docs.confluent.io/platform/current/installation/versions-interoperability.html) page for more details. 

-- @extref:[Javadoc for `KafkaConsumer`](kafka:/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html)

@@@

## Dependencies

@@dependency [Maven,sbt,Gradle] {
  group=com.typesafe.akka
  artifact=akka-stream-kafka_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2="$akka.version$"
  group2=com.typesafe.akka
  artifact2=akka-stream_$scala.binary.version$
  version2=PekkoVersion
}

This connector depends on Apache Pekko 2.6.x and note that it is important that all `akka-*` dependencies are in the same version, so it is recommended to depend on them explicitly to avoid problems with transient dependencies causing an unlucky mix of versions.

Apache Pekko Connectors Kafka APIs accept a typed @apidoc[org.apache.pekko.actor.typed.ActorSystem] or a classic @apidoc[org.apache.pekko.actor.ActorSystem] because both implement the @apidoc[org.apache.pekko.actor.ClassicActorSystemProvider] @scala[trait]@java[interface].
There are some Apache Pekko Connectors Kafka APIs that only accept classic a @apidoc[org.apache.pekko.actor.ActorRef], such as the @ref[rebalance listener](./consumer-rebalance.md) API, but otherwise there is no difference between running Apache Pekko Connectors Kafka and any other Apache Pekko Streams implementation with a typed @apidoc[org.apache.pekko.actor.typed.ActorSystem]. 
For more information on Apache Pekko classic and typed interoperability read the @extref[Coexistence](pekko:/typed/coexisting.html) page of the Apache Pekko Documentation.

The table below shows Apache Pekko Connectors Kafka's direct dependencies and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="core" }

* Apache Pekko Streams $akka.version$ @extref[documentation](pekko:stream/index.html) and [sources](https://github.com/akka/akka)
* Apache Kafka client $kafka.version$ @extref[documentation](kafka:/documentation#index) and [sources](https://github.com/apache/kafka)


## Scala and Java APIs

Following Apache Pekko's conventions there are two separate packages named `pekko.kafka.scaladsl` and `pekko.kafka.javadsl`
with the API for Scala and Java. These packages contain `Producer` and `Consumer`
classes with factory methods for the various Apache Pekko Streams `Flow`, `Sink` and `Source`
that are producing or consuming messages to/from Kafka.


## Examples

A few self-contained examples using Apache Pekko Connectors are available as [Apache Pekko Connectors Samples](https://akka.io/alpakka-samples/).

To read and see how others use Apache Pekko Connectors see the [Apache Pekko Connectors documentation's Webinars, Presentations and Articles](https://doc.akka.io/docs/alpakka/current/other-docs/webinars-presentations-articles.html) listing.


## Contributing

Please feel free to contribute to Apache Pekko Connectors and the Apache Pekko Connectors Kafka connector by reporting issues you identify, or by suggesting changes to the code. Please refer to our [contributing instructions](https://github.com/akka/alpakka-kafka/blob/master/CONTRIBUTING.md) to learn how it can be done.

We want Apache Pekko and Apache Pekko Connectors to strive in a welcoming and open atmosphere and expect all contributors to respect our [code of conduct](https://www.lightbend.com/conduct).


@@@ index

* [release notes](release-notes/index.md)

@@@
