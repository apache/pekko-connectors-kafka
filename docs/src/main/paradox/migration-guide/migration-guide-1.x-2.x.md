<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Migration from Apache Pekko Connectors Kafka 1.x to 2.x

Apache Pekko Connectors Kafka 2.x requires Apache Pekko 2.x. See the
@extref:[Pekko 1.x to 2.x migration guide](pekko:migration/migration-guide-1.x-2.x.html) for the changes in the core
Pekko libraries.

Apache Pekko Connectors Kafka 2.x is not binary compatible with 1.x. Libraries built against Pekko Connectors Kafka 1.x
or Pekko 1.x need to be rebuilt and released for Pekko 2.x.

## General changes

* Apache Pekko 2.x is the new minimum Pekko version.
* Java 17 is the new minimum Java version.
* Scala 2.12 is no longer supported. Scala 2.13 and Scala 3 are supported.
* The Kafka client has been upgraded from 3.9.x to 4.x. See the Kafka client section below.
* Deprecated code has been removed. Before upgrading, build against the latest Pekko Connectors Kafka 1.x release and
fix any deprecation warnings. The removed APIs and their replacements are listed below.

## Kafka client 4.x

Apache Pekko Connectors Kafka 2.x uses kafka-clients 4.x, where 1.x used kafka-clients 3.x.

* Kafka clients 4.x can only communicate with brokers that are version 2.1 or newer.
* Kafka 4.0 removed client APIs and configuration properties that were deprecated in the 3.x releases. If your code
uses the Kafka client API directly, for example in a custom `producerFactory` or `consumerFactory`, a
@apidoc[PartitionAssignmentHandler] or when setting Kafka properties on @apidoc[ConsumerSettings] or
@apidoc[ProducerSettings], review the [Kafka upgrade notes](https://kafka.apache.org/documentation/#upgrade).
* `RestrictedConsumer.committed(TopicPartition)` has been removed because the Kafka method it called no longer exists.
Use `committed(java.util.Set[TopicPartition])`.
* Libraries that depend on the Kafka client, such as Confluent's `kafka-avro-serializer`, need to be upgraded to
versions that support Kafka clients 4.x (Confluent Platform 8.x).

## Removed deprecated APIs

These APIs were deprecated in Alpakka Kafka or in Pekko Connectors Kafka 1.x and have been removed
([PR369](https://github.com/apache/pekko-connectors-kafka/pull/369)).

| Removed                                                                                   | Replacement
|-------------------------------------------------------------------------------------------|-------------
| `Committable.commitScaladsl()` and `Committable.commitJavadsl()`                           | `Committer.flow` or `Committer.sink`
| `Producer.committableSink(settings)` (Scala and Java DSL)                                  | `Producer.committableSink(producerSettings, committerSettings)`
| `Producer.flow(settings)` (Scala and Java DSL)                                             | `Producer.flexiFlow(settings)`
| `Producer.plainSink`, `committableSink`, `flow`, `flexiFlow` and `flowWithContext` overloads that take a Kafka `Producer` instance | Pass the producer in the settings using `ProducerSettings.withProducer` or `ProducerSettings.withProducerFactory`
| `ProducerSettings.producerFactory`                                                         | `createKafkaProducer()`, `createKafkaProducerAsync()` or `createKafkaProducerCompletionStage()`
| `SendProducer(settings, system)` with a classic `ActorSystem` (Scala DSL)                  | `SendProducer(settings)` with an implicit `ClassicActorSystemProvider`
| `Metadata.GetCommittedOffset`, `Metadata.CommittedOffset` and `Metadata.createGetCommittedOffset` | `Metadata.GetCommittedOffsets`, `Metadata.CommittedOffsets` and `Metadata.createGetCommittedOffsets`
| `MetadataClient.getCommittedOffset(partition)` (Scala and Java DSL)                        | `MetadataClient.getCommittedOffsets(partitions)`
| `RestrictedConsumer.committed(TopicPartition)`                                             | `RestrictedConsumer.committed(java.util.Set[TopicPartition])`

## Java DSL changes

* The Java DSL uses `java.lang.Long` for offsets where `scala.Long` leaked through as `Object`
([PR691](https://github.com/apache/pekko-connectors-kafka/pull/691)). Java code written against
`Map<TopicPartition, Object>` has to be changed to use `Map<TopicPartition, Long>`. This affects
    * the `getOffsetsOnAssign` parameter of `Consumer.plainPartitionedManualOffsetSource` and
      `Consumer.committablePartitionedManualOffsetSource`, which now returns a
      `CompletionStage<Map<TopicPartition, Long>>`
    * `CommittableOffsetBatch.getOffsets`, which now returns a `Map<GroupTopicPartition, Long>`
* `Consumer.committableExternalSource` has a new overload that takes a `java.time.Duration` commit timeout. The
overload that takes a Scala `FiniteDuration` is deprecated.
* `Producer.committableSink`, and the `metadataFromRecord`, `getOffsetsOnAssign` and `onRevoke` function parameters of
the `Consumer` sources, accept wildcard generic types. `Producer.committableSink` no longer needs an explicit type
witness. The erased signatures are unchanged.
* `TopicPartitionsAssigned` and `TopicPartitionsRevoked` have a new `getTopicPartitions` method that returns a
`java.util.Set`.

## Behavior changes

* @apidoc[ConsumerSettings], @apidoc[ProducerSettings] and @apidoc[CommitterSettings] validate their values. A
duration or count that is zero or negative, such as `poll-interval`, `stop-timeout`, `close-timeout`, `parallelism` or
`max-batch`, now fails with an `IllegalArgumentException` when the settings are created, instead of being accepted.
An infinite `commit-refresh-interval` is still valid.
([PR627](https://github.com/apache/pekko-connectors-kafka/pull/627))
* Offset commits that fail with a Kafka `TimeoutException` or `CoordinatorLoadInProgressException` are retried until
`commit-timeout` expires, as was already done for `RebalanceInProgressException`.
([PR505](https://github.com/apache/pekko-connectors-kafka/pull/505))
* The Java DSL `Consumer.DrainingControl.isShutdown` completes when the consumer has shut down and the stream has
completed, as the Scala DSL does. It previously completed as soon as the consumer had shut down. In both DSLs,
`isShutdown` returns the same @scala[`Future`]@java[`CompletionStage`] on every call.
([PR683](https://github.com/apache/pekko-connectors-kafka/pull/683))
* When the connection checker reports a `KafkaConnectionFailed`, the failure is also sent to the owner actor of the
`KafkaConsumerActor`, and not only to the stages that requested messages.
([PR626](https://github.com/apache/pekko-connectors-kafka/pull/626))

## Testkit

* The Testcontainers support can start Confluent Platform 8.x (Kafka 4) images, which run in KRaft mode without
ZooKeeper. The default `confluent-platform-version` is still a 7.x release (Kafka 3) and can be overridden with the
`CONFLUENT_PLATFORM_VERSION` environment variable or in the
@ref:[Testcontainers settings](../testing-testcontainers.md).
([PR646](https://github.com/apache/pekko-connectors-kafka/pull/646))
