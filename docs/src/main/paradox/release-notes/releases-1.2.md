# Release Notes (1.2.x)

## 1.2.0

Release notes for Apache Pekko Connectors Kafka 1.2.0.

### Bug Fix

* Clear lastRevoked after use to avoid wiping buffers under cooperative rebalancing [PR632](https://github.com/apache/pekko-connectors-kafka/pull/632)

### Dependency Change

* kafka-clients 3.9.2
