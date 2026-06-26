/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

// #metadataClient
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.kafka.ConsumerSettings;
import org.apache.pekko.kafka.javadsl.MetadataClient;
import org.apache.pekko.kafka.testkit.TestcontainersKafkaTest;
import org.apache.pekko.kafka.tests.javadsl.LogCapturingExtension;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
// #metadataClient
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(LogCapturingExtension.class)
class MetadataClientTest extends TestcontainersKafkaTest {

  private static final ActorSystem sys = ActorSystem.create("MetadataClientTest");
  private static final Executor executor = Executors.newSingleThreadExecutor();
  private static final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);

  MetadataClientTest() {
    super(sys);
  }

  @Test
  void shouldFetchBeginningOffsetsForGivenPartitions() {
    final String topic1 = createTopic();
    final String group1 = createGroupId();
    // #metadataClient
    final TopicPartition partition = new TopicPartition(topic1, 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final Set<TopicPartition> partitions = Set.of(partition);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    final CompletionStage<Map<TopicPartition, Long>> response =
        metadataClient.getBeginningOffsets(partitions);
    final Map<TopicPartition, Long> beginningOffsets = response.toCompletableFuture().join();
    // #metadataClient

    assertThat(beginningOffsets.get(partition), is(0L));

    // #metadataClient
    metadataClient.close();
    // #metadataClient
  }

  @Test
  void shouldFailInCaseOfAnExceptionDuringFetchBeginningOffsetsForNonExistingTopics() {
    final String group1 = createGroupId();
    final TopicPartition nonExistingPartition = new TopicPartition("non-existing topic", 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final Set<TopicPartition> partitions = Set.of(nonExistingPartition);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    final CompletionStage<Map<TopicPartition, Long>> response =
        metadataClient.getBeginningOffsets(partitions);

    metadataClient.close();

    CompletionException exception =
        assertThrows(CompletionException.class, () -> response.toCompletableFuture().join());
    assertInstanceOf(
        org.apache.kafka.common.errors.InvalidTopicException.class, exception.getCause());
  }

  @Test
  void shouldFetchBeginningOffsetForGivenPartition() {
    final String topic1 = createTopic();
    final String group1 = createGroupId();
    final TopicPartition partition = new TopicPartition(topic1, 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    final CompletionStage<Long> response = metadataClient.getBeginningOffsetForPartition(partition);
    final Long beginningOffset = response.toCompletableFuture().join();

    assertThat(beginningOffset, is(0L));

    metadataClient.close();
  }

  @Test
  void shouldFetchEndOffsetsForGivenPartitions() {
    final String topic1 = createTopic();
    final String group1 = createGroupId();
    final TopicPartition partition = new TopicPartition(topic1, 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    produceString(topic1, 10, partition.partition()).toCompletableFuture().join();

    final CompletionStage<Map<TopicPartition, Long>> response =
        metadataClient.getEndOffsets(Set.of(partition));
    final Map<TopicPartition, Long> endOffsets = response.toCompletableFuture().join();

    assertThat(endOffsets.get(partition), is(10L));

    metadataClient.close();
  }

  @Test
  void shouldFailInCaseOfAnExceptionDuringFetchEndOffsetsForNonExistingTopic() {
    final String group1 = createGroupId();
    final TopicPartition nonExistingPartition = new TopicPartition("non-existing topic", 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    final CompletionStage<Map<TopicPartition, Long>> response =
        metadataClient.getEndOffsets(Set.of(nonExistingPartition));

    metadataClient.close();

    CompletionException exception =
        assertThrows(CompletionException.class, () -> response.toCompletableFuture().join());
    assertInstanceOf(
        org.apache.kafka.common.errors.InvalidTopicException.class, exception.getCause());
  }

  @Test
  void shouldFetchEndOffsetForGivenPartition() {
    final String topic1 = createTopic();
    final String group1 = createGroupId();
    final TopicPartition partition = new TopicPartition(topic1, 0);
    final ConsumerSettings<String, String> consumerSettings =
        consumerDefaults().withGroupId(group1);
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    produceString(topic1, 10, partition.partition()).toCompletableFuture().join();

    final CompletionStage<Long> response = metadataClient.getEndOffsetForPartition(partition);
    final Long endOffset = response.toCompletableFuture().join();

    assertThat(endOffset, is(10L));
    metadataClient.close();
  }

  @Test
  void shouldFetchTopicList() {
    final String group = createGroupId();
    final String topic1 = createTopic(1, 2);
    final String topic2 = createTopic(2, 1);
    final ConsumerSettings<String, String> consumerSettings = consumerDefaults().withGroupId(group);
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    produceString(topic1, 10, 0).toCompletableFuture().join();
    produceString(topic1, 10, 1).toCompletableFuture().join();
    produceString(topic2, 10, 0).toCompletableFuture().join();

    final CompletionStage<Map<String, List<PartitionInfo>>> response = metadataClient.listTopics();

    final Map<String, List<PartitionInfo>> topics = response.toCompletableFuture().join();
    final Set<Integer> partitionsForTopic1 =
        topics.get(topic1).stream().map(PartitionInfo::partition).collect(toSet());
    final Set<Integer> partitionsForTopic2 =
        topics.get(topic2).stream().map(PartitionInfo::partition).collect(toSet());

    assertThat(partitionsForTopic1, containsInAnyOrder(0, 1));
    assertThat(partitionsForTopic2, containsInAnyOrder(0));

    metadataClient.close();
  }

  @Test
  void shouldFetchPartitionsInfoForGivenTopic() {
    final String group = createGroupId();
    final String topic = createTopic(1, 2);
    final ConsumerSettings<String, String> consumerSettings = consumerDefaults().withGroupId(group);
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
    final MetadataClient metadataClient =
        MetadataClient.create(consumerSettings, timeout, sys, executor);

    produceString(topic, 10, 0).toCompletableFuture().join();
    produceString(topic, 10, 1).toCompletableFuture().join();

    final CompletionStage<List<PartitionInfo>> response = metadataClient.getPartitionsFor(topic);

    final List<PartitionInfo> partitionInfos = response.toCompletableFuture().join();
    final Set<Integer> partitions =
        partitionInfos.stream().map(PartitionInfo::partition).collect(toSet());

    assertThat(partitions, containsInAnyOrder(0, 1));

    metadataClient.close();
  }

  @AfterAll
  void afterClass() {
    TestKit.shutdownActorSystem(sys);
  }
}
