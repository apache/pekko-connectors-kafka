/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package docs.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.kafka.AutoSubscription;
import org.apache.pekko.kafka.CommitterSettings;
import org.apache.pekko.kafka.ConsumerMessage;
import org.apache.pekko.kafka.ConsumerSettings;
import org.apache.pekko.kafka.ProducerMessage;
import org.apache.pekko.kafka.ProducerSettings;
import org.apache.pekko.kafka.Subscriptions;
import org.apache.pekko.kafka.TopicPartitionsAssigned;
import org.apache.pekko.kafka.TopicPartitionsRevoked;
import org.apache.pekko.kafka.javadsl.Consumer;
import org.apache.pekko.kafka.javadsl.Producer;
import org.apache.pekko.kafka.testkit.ConsumerResultFactory;
import org.apache.pekko.kafka.tests.javadsl.LogCapturingExtension;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Most of these tests guard the Java signatures: they fail to compile if the signatures regress.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(LogCapturingExtension.class)
class JavaApiTypesTest {

  private static final ActorSystem sys = ActorSystem.create("JavaApiTypesTest");
  private static final TopicPartition tp = new TopicPartition("topic", 0);

  private final ConsumerSettings<String, String> consumerSettings =
      ConsumerSettings.create(sys, new StringDeserializer(), new StringDeserializer());
  private final AutoSubscription subscription = Subscriptions.topics("topic");

  @AfterAll
  void afterAll() {
    TestKit.shutdownActorSystem(sys);
  }

  private static CompletionStage<Map<TopicPartition, Long>> offsets(Set<TopicPartition> tps) {
    return CompletableFuture.completedFuture(Map.of(tp, 1L));
  }

  private static CompletableFuture<HashMap<TopicPartition, Long>> offsetsFromCollection(
      Collection<TopicPartition> tps) {
    return CompletableFuture.completedFuture(new HashMap<>());
  }

  private static String metadata(ConsumerRecord<?, ?> record) {
    return record.topic();
  }

  @Test
  void manualOffsetSourcesAcceptJavaLongOffsets() {
    assertNotNull(
        Consumer.plainPartitionedManualOffsetSource(
            consumerSettings, subscription, JavaApiTypesTest::offsets));
    assertNotNull(
        Consumer.committablePartitionedManualOffsetSource(
            consumerSettings, subscription, JavaApiTypesTest::offsets));
  }

  @Test
  void manualOffsetSourcesAcceptWiderFunctions() {
    final Function<Collection<TopicPartition>, CompletableFuture<HashMap<TopicPartition, Long>>>
        getOffsets = JavaApiTypesTest::offsetsFromCollection;
    final java.util.function.Consumer<Collection<TopicPartition>> onRevoke = tps -> {};
    assertNotNull(
        Consumer.plainPartitionedManualOffsetSource(
            consumerSettings, subscription, getOffsets, onRevoke));
    assertNotNull(
        Consumer.committablePartitionedManualOffsetSource(
            consumerSettings, subscription, getOffsets, onRevoke));
  }

  @Test
  void metadataFunctionsMayAcceptWiderRecordTypes() {
    final Function<ConsumerRecord<?, ?>, String> metadata = JavaApiTypesTest::metadata;
    assertNotNull(Consumer.commitWithMetadataSource(consumerSettings, subscription, metadata));
    assertNotNull(
        Consumer.commitWithMetadataPartitionedSource(consumerSettings, subscription, metadata));
    assertNotNull(Consumer.sourceWithOffsetContext(consumerSettings, subscription, metadata));
  }

  @Test
  void committableSinkAcceptsEnvelopesOfCommittableSubtypes() {
    final ProducerSettings<String, String> producerSettings =
        ProducerSettings.create(sys, new StringSerializer(), new StringSerializer());
    final CommitterSettings committerSettings = CommitterSettings.create(sys);
    final Source<
            ProducerMessage.Envelope<String, String, ConsumerMessage.CommittableOffset>, NotUsed>
        source = Source.empty();
    assertNotNull(source.to(Producer.committableSink(producerSettings, committerSettings)));
  }

  @Test
  void committableExternalSourceAcceptsJavaDuration() {
    final Source<ConsumerMessage.CommittableMessage<String, String>, Consumer.Control> source =
        Consumer.committableExternalSource(
            sys.deadLetters(), Subscriptions.assignment(tp), "group", Duration.ofSeconds(1));
    assertNotNull(source);
  }

  @Test
  void committableOffsetBatchReturnsJavaLongOffsets() {
    final ConsumerMessage.CommittableOffset offset =
        ConsumerResultFactory.committableOffset("group", "topic", 0, 42L, "");
    final Map<ConsumerMessage.GroupTopicPartition, Long> offsets =
        ConsumerMessage.createCommittableOffsetBatch(offset).getOffsets();
    assertEquals(
        Long.valueOf(42L),
        offsets.get(new ConsumerMessage.GroupTopicPartition("group", "topic", 0)));
  }

  @Test
  void rebalanceEventsExposeJavaSets() {
    final scala.collection.immutable.Set<TopicPartition> tps =
        scala.jdk.javaapi.CollectionConverters.asScala(Set.of(tp)).toSet();
    assertEquals(Set.of(tp), new TopicPartitionsAssigned(subscription, tps).getTopicPartitions());
    assertEquals(Set.of(tp), new TopicPartitionsRevoked(subscription, tps).getTopicPartitions());
  }
}
