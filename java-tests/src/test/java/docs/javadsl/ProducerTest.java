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

import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.kafka.*;
import org.apache.pekko.kafka.javadsl.Consumer;
import org.apache.pekko.kafka.javadsl.Producer;
// #testkit
import org.apache.pekko.kafka.testkit.TestcontainersKafkaTest;
// #testkit
import org.apache.pekko.kafka.tests.javadsl.LogCapturingExtension;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
// #testkit
import org.apache.pekko.testkit.javadsl.TestKit;
// #testkit
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
// #testkit
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
// #testkit

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

// #testkit

@TestInstance(Lifecycle.PER_CLASS)
// #testkit
@ExtendWith(LogCapturingExtension.class)
// #testkit
class ProducerTest extends TestcontainersKafkaTest {

  private static final ActorSystem system = ActorSystem.create();
  // #testkit

  private final Executor executor = Executors.newSingleThreadExecutor();
  private final ProducerSettings<String, String> producerSettings = producerDefaults();

  // #testkit

  ProducerTest() {
    super(system);
  }

  @AfterAll
  void shutdownActorSystem() {
    TestKit.shutdownActorSystem(system);
  }

  // #testkit
  @Test
  void createProducer() {
    // #producer
    // #settings
    final Config config = system.settings().config().getConfig("pekko.kafka.producer");
    final ProducerSettings<String, String> producerSettings =
        ProducerSettings.create(config, new StringSerializer(), new StringSerializer())
            .withBootstrapServers("localhost:9092");
    // #settings
    final org.apache.kafka.clients.producer.Producer<String, String> kafkaProducer =
        producerSettings.createKafkaProducer();
    // #producer
    kafkaProducer.close();
  }

  @Test
  void plainSink() throws Exception {
    String topic = createTopic();
    // #plainSink
    CompletionStage<Done> done =
        Source.range(1, 100)
            .map(number -> number.toString())
            .map(value -> new ProducerRecord<String, String>(topic, value))
            .runWith(Producer.plainSink(producerSettings), system);
    // #plainSink

    Consumer.DrainingControl<List<ConsumerRecord<String, String>>> control =
        consumeString(topic, 100);
    assertEquals(Done.done(), resultOf(done));
    assertEquals(Done.done(), resultOf(control.isShutdown()));
    CompletionStage<List<ConsumerRecord<String, String>>> result =
        control.drainAndShutdown(executor);
    assertEquals(100, resultOf(result).size());
  }

  @Test
  void plainSinkWithSharedProducer() throws Exception {
    String topic = createTopic();
    final org.apache.kafka.clients.producer.Producer<String, String> kafkaProducer =
        producerSettings.createKafkaProducer();
    // #plainSinkWithProducer
    ProducerSettings<String, String> settingsWithProducer =
        producerSettings.withProducer(kafkaProducer);

    CompletionStage<Done> done =
        Source.range(1, 100)
            .map(number -> number.toString())
            .map(value -> new ProducerRecord<String, String>(topic, value))
            .runWith(Producer.plainSink(settingsWithProducer), system);
    // #plainSinkWithProducer

    Consumer.DrainingControl<List<ConsumerRecord<String, String>>> control =
        consumeString(topic, 100);
    assertEquals(Done.done(), resultOf(done));
    assertEquals(Done.done(), resultOf(control.isShutdown()));
    CompletionStage<List<ConsumerRecord<String, String>>> result =
        control.drainAndShutdown(executor);
    assertEquals(100, resultOf(result).size());

    kafkaProducer.close();
  }

  @Test
  void observeMetrics() throws Exception {
    final org.apache.kafka.clients.producer.Producer<String, String> kafkaProducer =
        producerSettings.createKafkaProducer();
    // #producerMetrics
    Map<org.apache.kafka.common.MetricName, ? extends org.apache.kafka.common.Metric> metrics =
        kafkaProducer.metrics(); // observe metrics
    // #producerMetrics
    assertFalse(metrics.isEmpty());
    kafkaProducer.close();
  }

  <KeyType, ValueType, PassThroughType>
      ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> createMessage(
          KeyType key, ValueType value, PassThroughType passThrough) {
    // #singleMessage
    ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> message =
        ProducerMessage.single(new ProducerRecord<>("topicName", key, value), passThrough);
    // #singleMessage
    return message;
  }

  <KeyType, ValueType, PassThroughType>
      ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> createMultiMessage(
          KeyType key, ValueType value, PassThroughType passThrough) {
    // #multiMessage
    ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> multiMessage =
        ProducerMessage.multi(
            Arrays.asList(
                new ProducerRecord<>("topicName", key, value),
                new ProducerRecord<>("anotherTopic", key, value)),
            passThrough);
    // #multiMessage
    return multiMessage;
  }

  <KeyType, ValueType, PassThroughType>
      ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> createPassThroughMessage(
          KeyType key, ValueType value, PassThroughType passThrough) {
    // #passThroughMessage
    ProducerMessage.Envelope<KeyType, ValueType, PassThroughType> ptm =
        ProducerMessage.passThrough(passThrough);
    // #passThroughMessage
    return ptm;
  }

  @Test
  void producerFlowExample() throws Exception {
    String topic = createTopic();
    // #flow
    CompletionStage<Done> done =
        Source.range(1, 100)
            .map(
                number -> {
                  int partition = 0;
                  String value = String.valueOf(number);
                  ProducerMessage.Envelope<String, String, Integer> msg =
                      ProducerMessage.single(
                          new ProducerRecord<>(topic, partition, "key", value), number);
                  return msg;
                })
            .via(Producer.flexiFlow(producerSettings))
            .map(
                result -> {
                  if (result instanceof ProducerMessage.Result<String, String, Integer> res) {
                    ProducerRecord<String, String> record = res.message().record();
                    RecordMetadata meta = res.metadata();
                    return meta.topic()
                        + "/"
                        + meta.partition()
                        + " "
                        + res.offset()
                        + ": "
                        + record.value();
                  } else if (result instanceof ProducerMessage.MultiResult<String, String, Integer> res) {
                    return res.getParts().stream()
                        .map(
                            part -> {
                              RecordMetadata meta = part.metadata();
                              return meta.topic()
                                  + "/"
                                  + meta.partition()
                                  + " "
                                  + part.metadata().offset()
                                  + ": "
                                  + part.record().value();
                            })
                        .reduce((acc, s) -> acc + ", " + s);
                  } else {
                    return "passed through";
                  }
                })
            .runWith(Sink.foreach(System.out::println), system);
    // #flow

    Consumer.DrainingControl<List<ConsumerRecord<String, String>>> control =
        consumeString(topic, 100L);
    assertEquals(Done.done(), resultOf(done));
    assertEquals(Done.done(), resultOf(control.isShutdown()));
    CompletionStage<List<ConsumerRecord<String, String>>> result =
        control.drainAndShutdown(executor);
    assertEquals(100, resultOf(result).size());
  }
  // #testkit
}
// #testkit
