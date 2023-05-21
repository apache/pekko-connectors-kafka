# Apache Pekko Connectors Kafka Documentation

The [Apache Pekko Connectors project](https://pekko.apache.org/docs/pekko-connectors-kafka/current/) is an open source initiative to implement stream-aware and reactive integration pipelines for Java and Scala. It is built on top of @extref[Pekko Streams](pekko:stream/index.html), and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Apache Pekko Streams is a [Reactive Streams](https://www.reactive-streams.org/) and JDK 9+ @extref[java.util.concurrent.Flow](java-docs:docs/api/java.base/java/util/concurrent/Flow.html)-compliant implementation and therefore @extref[fully interoperable](pekko:general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

This **Apache Pekko Connectors Kafka connector** lets you connect [Apache Kafka](https://kafka.apache.org/) to Apache Pekko Streams. This project is a fork of [Alpakka Kafka](https://github.com/akka/alpakka-kafka).

@@toc { .main depth=2 }

@@@ index

* [overview](home.md)
* [Producer](producer.md)
* [Consumer](consumer.md)
* [Discovery](discovery.md)
* [Cluster Sharding](cluster-sharding.md)
* [Error Handling](errorhandling.md)
* [At-Least-Once Delivery](atleastonce.md)
* [Transactions](transactions.md)
* [deser](serialization.md)
* [debug](debugging.md)
* [test](testing.md)
* [test-testcontainers](testing-testcontainers.md)
* [in prod](production.md)
* [Snapshots](snapshots.md)

@@@
