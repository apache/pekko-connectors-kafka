/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.kafka.CommitterSettings;
import org.apache.pekko.kafka.ConsumerMessage;
import org.apache.pekko.kafka.ProducerMessage;
import org.apache.pekko.kafka.javadsl.Committer;
import org.apache.pekko.kafka.javadsl.Consumer;
import org.apache.pekko.kafka.tests.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

// #factories
import org.apache.pekko.kafka.testkit.ConsumerResultFactory;
import org.apache.pekko.kafka.testkit.ProducerResultFactory;
import org.apache.pekko.kafka.testkit.javadsl.ConsumerControlFactory;
// #factories

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class TestkitSamplesTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static final ActorSystem sys = ActorSystem.create("TestkitSamplesTest");

  @AfterClass
  public static void afterClass() {
    TestKit.shutdownActorSystem(sys);
  }

  @Test
  public void withoutBrokerTesting() throws Exception {
    String topic = "topic";
    String targetTopic = "target-topic";
    String groupId = "group1";
    long startOffset = 100L;
    int partition = 0;
    CommitterSettings committerSettings = CommitterSettings.create(sys);

    // #factories

    // create elements emitted by the mocked Consumer
    List<ConsumerMessage.CommittableMessage<String, String>> elements =
        Arrays.asList(
            ConsumerResultFactory.committableMessage(
                new ConsumerRecord<>(topic, partition, startOffset, "key", "value 1"),
                ConsumerResultFactory.committableOffset(
                    groupId, topic, partition, startOffset, "metadata")),
            ConsumerResultFactory.committableMessage(
                new ConsumerRecord<>(topic, partition, startOffset + 1, "key", "value 2"),
                ConsumerResultFactory.committableOffset(
                    groupId, topic, partition, startOffset + 1, "metadata 2")));

    // create a source imitating the Consumer.committableSource
    Source<ConsumerMessage.CommittableMessage<String, String>, Consumer.Control>
        mockedKafkaConsumerSource =
            Source.cycle(elements::iterator)
                .viaMat(ConsumerControlFactory.controlFlow(), Keep.right());

    // create a source imitating the Producer.flexiFlow
    Flow<
            ProducerMessage.Envelope<String, String, ConsumerMessage.CommittableOffset>,
            ProducerMessage.Results<String, String, ConsumerMessage.CommittableOffset>,
            NotUsed>
        mockedKafkaProducerFlow =
            Flow
                .<ProducerMessage.Envelope<String, String, ConsumerMessage.CommittableOffset>>
                    create()
                .map(
                    msg -> {
                      if (msg instanceof ProducerMessage.Message) {
                        ProducerMessage.Message<String, String, ConsumerMessage.CommittableOffset>
                            msg2 =
                                (ProducerMessage.Message<
                                        String, String, ConsumerMessage.CommittableOffset>)
                                    msg;
                        return ProducerResultFactory.result(msg2);
                      } else throw new RuntimeException("unexpected element: " + msg);
                    });

    // run the flow as if it was connected to a Kafka broker
    Pair<Consumer.Control, CompletionStage<Done>> stream =
        mockedKafkaConsumerSource
            .map(
                msg -> {
                  ProducerMessage.Envelope<String, String, ConsumerMessage.CommittableOffset>
                      message =
                          new ProducerMessage.Message<>(
                              new ProducerRecord<>(
                                  targetTopic, msg.record().key(), msg.record().value()),
                              msg.committableOffset());
                  return message;
                })
            .via(mockedKafkaProducerFlow)
            .map(ProducerMessage.Results::passThrough)
            .toMat(Committer.sink(committerSettings), Keep.both())
            .run(sys);
    // #factories

    Thread.sleep(1 * 1000L);
    assertThat(
        stream.first().shutdown().toCompletableFuture().get(2, TimeUnit.SECONDS),
        is(Done.getInstance()));
    assertThat(
        stream.second().toCompletableFuture().get(2, TimeUnit.SECONDS), is(Done.getInstance()));
  }
}
