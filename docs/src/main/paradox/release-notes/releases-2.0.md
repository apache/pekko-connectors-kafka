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

# Release Notes (2.0.x)

@@@ warning

Pekko Connectors Kafka 2.x and Pekko 2.x are not binary compatible with their 1.x releases. Libraries built against
Pekko 1.x or Pekko Connectors Kafka 1.x cannot be trusted to work with Pekko Connectors Kafka 2.x, and need to be
rebuilt and released for Pekko 2.x. See the @extref:[Pekko 2.0 release notes](pekko:release-notes/releases-2.0.html)
for the binary compatibility changes in the core Pekko modules.

Read the @ref:[migration guide](../migration-guide/migration-guide-1.x-2.x.md) before upgrading from
Pekko Connectors Kafka 1.x.

@@@

## 2.0.0

Release notes for Apache Pekko Connectors Kafka 2.0.0. This is the first release of a new major version and includes
the changes from the 2.0.0-M1 milestone release. See the GitHub milestones for
[2.0.0-M1](https://github.com/apache/pekko-connectors-kafka/milestone/4?closed=1) and
[2.0.0-M2](https://github.com/apache/pekko-connectors-kafka/milestone/5?closed=1) for a fuller list of changes.

### Main changes

* Pekko 2.0.0 is the new minimum Pekko version
* Java 17 is the new minimum Java version
* Scala 2.12 support has been dropped
* The Kafka client has been upgraded to 4.x ([PR358](https://github.com/apache/pekko-connectors-kafka/pull/358))
* Deprecated code has been removed ([PR369](https://github.com/apache/pekko-connectors-kafka/pull/369))
* The Java DSL uses `java.lang.Long` for offsets, accepts `java.time.Duration` and has wider generic parameter types ([PR691](https://github.com/apache/pekko-connectors-kafka/pull/691))
* `ConsumerSettings`, `ProducerSettings` and `CommitterSettings` reject durations and counts that are not positive ([PR627](https://github.com/apache/pekko-connectors-kafka/pull/627))

### Additions

* `ConsumerSettings.withPartitionAssignmentStrategies` and `withPartitionAssignmentStrategyCooperativeStickyAssignor` to select the partition assignment strategies ([PR506](https://github.com/apache/pekko-connectors-kafka/pull/506))
* The testkit can start Confluent Platform 8.x (Kafka 4) containers in KRaft mode ([PR646](https://github.com/apache/pekko-connectors-kafka/pull/646))

### Bug Fixes

* Retry offset commits that fail with a transient `TimeoutException` or `CoordinatorLoadInProgressException` ([PR505](https://github.com/apache/pekko-connectors-kafka/pull/505))
* Clear lastRevoked after use to avoid wiping buffers under cooperative rebalancing ([PR617](https://github.com/apache/pekko-connectors-kafka/pull/617))
* Transactional producer: abort the transaction when the final commit fails as the stage completes, and do not let a failing abort hide the original stage failure ([PR623](https://github.com/apache/pekko-connectors-kafka/pull/623))
* Transactional sources wait in the partition revoke callback until the in-flight records of the revoked partitions are drained, so records are not duplicated across a rebalance. The wait is marked as blocking and the dispatcher sizing it needs is described in the @ref:[transactions](../transactions.md#consume-transform-produce-workflow) documentation ([PR695](https://github.com/apache/pekko-connectors-kafka/pull/695))
* Implement `hashCode` on the committer used by committable offsets, consistent with its `equals` ([PR625](https://github.com/apache/pekko-connectors-kafka/pull/625))
* Propagate `KafkaConnectionFailed` to the owner actor of the consumer actor ([PR626](https://github.com/apache/pekko-connectors-kafka/pull/626))
* Close Kafka producers that are created after their stage has completed, and close the Kafka consumer when a partition assignment handler fails in `onStop` ([PR650](https://github.com/apache/pekko-connectors-kafka/pull/650))
* Release all testkit resources when cleanup or cluster startup fails ([PR651](https://github.com/apache/pekko-connectors-kafka/pull/651))
* `DrainingControl.isShutdown` attaches its callbacks only once, and the Java DSL version waits for the stream to complete ([PR683](https://github.com/apache/pekko-connectors-kafka/pull/683))
* Let a transactional consumer leave its group promptly when the source stage stops before the consumer actor ([PR697](https://github.com/apache/pekko-connectors-kafka/pull/697))

### Dependency Changes

* kafka-clients 4.4.0
