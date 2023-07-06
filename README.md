# Apache Pekko Kafka Connector [![scaladex-badge][]][scaladex] [![maven-central-badge][]][maven-central] [![gh-actions-badge][]][gh-actions]

[scaladex]:            https://index.scala-lang.org/apache/incubator-pekko-connectors-kafka/
[scaladex-badge]:      https://index.scala-lang.org/apache/incubator-pekko-connectors-kafka/latest.svg?target=_2.13
[maven-central]:       https://maven-badges.herokuapp.com/maven-central/org.apache.pekko/pekko-connectors-kafka_2.13
[maven-central-badge]: https://maven-badges.herokuapp.com/maven-central/org.apache.pekko/pekko-connectors-kafka_2.13/badge.svg
[gh-actions]:          https://github.com/apache/incubator-pekko-connectors-kafka/actions
[gh-actions-badge]:    https://github.com/apache/incubator-pekko-connectors-kafka/workflows/CI/badge.svg?branch=main


Systems don't come alone. In the modern world of microservices and cloud deployment, new components must interact with legacy systems, making integration an important key to success. Reactive Streams give us a technology-independent tool to let these heterogeneous systems communicate without overwhelming each other.

The Apache Pekko Connectors project is an open source initiative to implement stream-aware, reactive, integration pipelines for Java and Scala. It is built on top of [Apache Pekko Streams](https://pekko.apache.org/docs/pekko/current/stream/index.html), and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Pekko Streams is a [Reactive Streams](https://www.reactive-streams.org/) and JDK 9+ [java.util.concurrent.Flow](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/Flow.html)-compliant implementation and therefore [fully interoperable](https://pekko.apache.org/docs/pekko/current/general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

This repository contains the sources for the **Apache Pekko Kafka Connector**. Which lets you connect [Apache Kafka](https://kafka.apache.org/) to Pekko Streams.

Pekko connectors to other technologies are listed in the [Pekko connectors repository](https://github.com/apache/incubator-pekko-connectors).

The Apache Pekko Kafka Connector is a fork of [Alpakka Kafka](https://github.com/akka/alpakka-kafka) 3.0.1, prior to the Akka project's adoption of the Business Source License.

## Reference Documentation

See https://pekko.apache.org for the documentation including the API docs. The docs for all the Apache Pekko modules can be found there.

## Community

You can join these forums and chats to discuss and ask Pekko and Pekko connector related questions:

- [GitHub discussions](https://github.com/apache/incubator-pekko-connectors-kafka/discussions): for questions and general discussion.
- [Pekko dev mailing list](https://lists.apache.org/list.html?dev@pekko.apache.org): for Pekko development discussions.
- [Pekko users mailing list](https://lists.apache.org/list.html?users@pekko.apache.org): for Pekko user discussions.
- [GitHub issues](https://github.com/apache/incubator-pekko-connectors-kafka/issues): for bug reports and feature requests. Please search the existing issues before creating new ones. If you are unsure whether you have found a bug, consider asking in GitHub discussions or the mailing list first.

## Contributing

Contributions are very welcome. If you have an idea on how to improve Pekko, don't hesitate to create an issue or submit a pull request.

See [CONTRIBUTING.md](CONTRIBUTING.md) for details on the development workflow and how to create your pull request.

## Caveat Emptor

Pekko Connectors are not always binary compatible between releases. API changes that are not backward compatible might be introduced as we refine and simplify based on your feedback. A module may be dropped in any release without prior deprecation.
