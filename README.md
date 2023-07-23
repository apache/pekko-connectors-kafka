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

## Building from Source

### Prerequisites
- Make sure you have installed a Java Development Kit (JDK) version 8 or later.
- Make sure you have [sbt](https://www.scala-sbt.org/) installed and using this JDK.
- [Graphviz](https://graphviz.gitlab.io/download/) is needed for the scaladoc generation build task, which is part of the release.
- Running [Docker Engine](https://docs.docker.com/engine/) is required for many of the unit tests.

### Running the Build
- Open a command window and change directory to your preferred base directory
- Use git to clone the [repo](https://github.com/apache/incubator-pekko-connectors-kafka) or download a source release from https://pekko.apache.org (and unzip or untar it, as appropriate)
- Change directory to the directory where you installed the source (you should have a file called `build.sbt` in this directory)
- `sbt compile` compiles the main source for project default version of Scala (2.13)
    - `sbt +compile` will compile for all supported versions of Scala
- `sbt test` will compile the code and run the unit tests
  - check that Docker is running first
  - The [testing](https://pekko.apache.org/docs/pekko-connectors-kafka/current/testing.html) docs are a useful guide
- `sbt testQuick` similar to test but when repeated in shell mode will only run failing tests
- `sbt package` will build the jars
    - the jars will built into target dirs of the various modules
    - for the the 'core' module, the jar will be built to `core/target/scala-2.13/`
- `sbt publishLocal` will push the jars to your local Apache Ivy repository
- `sbt publishM2` will push the jars to your local Apache Maven repository
- `sbt docs/paradox` will build the docs (the ones describing the module features)
     - `sbt docs/paradoxBrowse` does the same but will open the docs in your browser when complete
     - the `index.html` file will appear in `target/paradox/site/main/`
- `sbt unidoc` will build the Javadocs for all the modules and load them to one place (may require Graphviz, see Prerequisites above)
     - the `index.html` file will appear in `target/scala-2.13/unidoc/`
- `sbt sourceDistGenerate` will generate source release to `target/dist/`
- The version number that appears in filenames and docs is derived, by default. The derived version contains the most git commit id or the date/time (if the directory is not under git control). 
    - You can set the version number explicitly when running sbt commands
        - eg `sbt "set ThisBuild / version := \"1.0.0\"; sourceDistGenerate"`  
    - Or you can add a file called `version.sbt` to the same directory that has the `build.sbt` containing something like
        - `ThisBuild / version := "1.0.0"`

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
