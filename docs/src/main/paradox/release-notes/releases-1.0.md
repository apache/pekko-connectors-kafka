# Release Notes (1.0.x)

## 1.0.0
Apache Pekko Connectors Kafka 1.0.0 is based on Alpakka Kafka 3.0.1. Pekko came about as a result of Lightbend's decision to make future
Akka releases under a [Business Software License](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka),
a license that is not compatible with Open Source usage.

Apache Pekko has changed the package names, among other changes. The new packages begin with `org.apache.pekko.kafka` instead of `akka.kafka`.

Config names have changed to use `pekko` instead of `akka` in their names.

Users switching from Akka to Pekko should read our [Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html).

Generally, we have tried to make it as easy as possible to switch existing Akka based projects over to using Pekko.

We have gone through the code base and have tried to properly acknowledge all third party source code in the
Apache Pekko code base. If anyone believes that there are any instances of third party source code that is not
properly acknowledged, please get in touch.

### Bug Fixes
We haven't had to fix many significant bugs that were in Alpakka Kafka 3.0.1.

* Properly mask all sensitive fields in Consumer and Producer settings [PR100](https://github.com/apache/pekko-connectors-kafka/pull/100) (Alpakka Kafka [CVE-2023-29471](https://akka.io/security/alpakka-kafka-cve-2023-29471.html))

### Additions

* Scala 3 support ([PR58](https://github.com/apache/pekko-connectors-kafka/pull/58))
    * minimum version of Scala 3.3.0 required 

### Dependency Upgrades
We have tried to limit the changes to third party dependencies that were used in Alpakka Kafka 3.0.1. These are some exceptions:

* jackson 2.14.3
* protobuf-java 3.19.6
* scalatest 3.2.14. Pekko users who have existing tests based on Akka Testkit may need to migrate their tests due to the scalatest upgrade. The [scalatest 3.2 release notes](https://www.scalatest.org/release_notes/3.2.0) have a detailed description of the changes needed.
