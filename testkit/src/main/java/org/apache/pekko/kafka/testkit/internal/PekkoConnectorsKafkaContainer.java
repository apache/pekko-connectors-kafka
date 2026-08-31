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

package org.apache.pekko.kafka.testkit.internal;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.pekko.annotation.InternalApi;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * This container wraps Confluent Kafka and Zookeeper (optionally)
 *
 * <p>This is a copy of KafkaContainer from testcontainers/testcontainers-java that we can tweak as
 * needed
 */
@InternalApi
public class PekkoConnectorsKafkaContainer extends GenericContainer<PekkoConnectorsKafkaContainer> {

  private static final String START_STOP_SCRIPT = "/testcontainers_start_stop_wrapper.sh";

  private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

  // Align these confluent platform constants with testkit/src/main/resources/reference.conf
  public static final String DEFAULT_CONFLUENT_PLATFORM_VERSION =
      System.getProperty(
          "CONFLUENT_PLATFORM_VERSION",
          System.getenv().getOrDefault("CONFLUENT_PLATFORM_VERSION", "7.9.2"));

  public static final DockerImageName DEFAULT_ZOOKEEPER_IMAGE_NAME =
      DockerImageName.parse("confluentinc/cp-zookeeper")
          .withTag(DEFAULT_CONFLUENT_PLATFORM_VERSION);

  public static final DockerImageName DEFAULT_KAFKA_IMAGE_NAME =
      DockerImageName.parse("confluentinc/cp-kafka").withTag(DEFAULT_CONFLUENT_PLATFORM_VERSION);

  public static final int KAFKA_PORT = 9093;

  public static final int KAFKA_JMX_PORT = 49999;

  public static final int ZOOKEEPER_PORT = 2181;

  public static final int KAFKA_CONTROLLER_PORT = 9094;

  // matches the default cluster id used by testcontainers' KafkaContainer
  public static final String DEFAULT_CLUSTER_ID = "4L6g3nShT-eMCtK--X86sw";

  private static final int PORT_NOT_ASSIGNED = -1;

  protected String externalZookeeperConnect = null;

  private int brokerNum = 1;

  private int port = PORT_NOT_ASSIGNED;
  private int jmxPort = PORT_NOT_ASSIGNED;

  private boolean useImplicitNetwork = true;

  private boolean enableRemoteJmxService = false;

  private final boolean useKraft;

  public PekkoConnectorsKafkaContainer() {
    this(DEFAULT_KAFKA_IMAGE_NAME);
  }

  public PekkoConnectorsKafkaContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);

    // Kafka 4 (Confluent Platform 8.x) has no ZooKeeper support, brokers must run in KRaft mode
    this.useKraft = requiresKraftMode(dockerImageName.getVersionPart());

    super.withNetwork(Network.SHARED);

    withExposedPorts(KAFKA_PORT);

    // The default startup timeout of 1 minute is not enough for Kafka to start reliably in CI
    withStartupTimeout(Duration.ofMinutes(3));

    withBrokerNum(this.brokerNum);

    // Use two listeners with different names, it will force Kafka to communicate with itself via
    // internal
    // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the
    // advertised listener
    withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
    withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
    withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

    withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
    withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
    withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
    withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
  }

  private static boolean requiresKraftMode(String imageVersionPart) {
    if (imageVersionPart == null) {
      return false;
    }
    int dotIndex = imageVersionPart.indexOf('.');
    String major = dotIndex > 0 ? imageVersionPart.substring(0, dotIndex) : imageVersionPart;
    try {
      return Integer.parseInt(major) >= 8;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public boolean usesKraftMode() {
    return useKraft;
  }

  @Override
  public PekkoConnectorsKafkaContainer withNetwork(Network network) {
    useImplicitNetwork = false;
    return super.withNetwork(network);
  }

  public PekkoConnectorsKafkaContainer withBrokerNum(int brokerNum) {
    // getNetworkAliases() returns a copy, so build the new alias list and set it explicitly
    // to make sure the alias of the previous broker number is dropped
    List<String> aliases = getNetworkAliases();
    aliases.remove("broker-" + this.brokerNum);
    this.brokerNum = brokerNum;
    if (!aliases.contains("broker-" + brokerNum)) {
      aliases.add("broker-" + brokerNum);
    }
    setNetworkAliases(aliases);
    return withEnv("KAFKA_BROKER_ID", "" + this.brokerNum);
  }

  @Override
  public Network getNetwork() {
    if (useImplicitNetwork) {
      // TODO Only for backward compatibility, to be removed soon
      logger()
          .warn(
              "Deprecation warning! "
                  + "KafkaContainer#getNetwork without an explicitly set network. "
                  + "Consider using KafkaContainer#withNetwork",
              new Exception("Deprecated method"));
    }
    return super.getNetwork();
  }

  public int getBrokerNum() {
    return brokerNum;
  }

  public void stopKafka() {
    try {
      ExecResult execResult = execInContainer("sh", "-c", "touch /tmp/stop");
      if (execResult.getExitCode() != 0) throw new Exception(execResult.getStderr());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void startKafka() {
    try {
      ExecResult execResult = execInContainer("sh", "-c", "touch /tmp/start");
      if (execResult.getExitCode() != 0) throw new Exception(execResult.getStderr());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public PekkoConnectorsKafkaContainer withEmbeddedZookeeper() {
    externalZookeeperConnect = null;
    return self();
  }

  public PekkoConnectorsKafkaContainer withExternalZookeeper(String connectString) {
    if (useKraft) {
      throw new IllegalStateException(
          "ZooKeeper is not supported with Kafka 4 (Confluent Platform 8.x) images, "
              + "the broker runs in KRaft mode");
    }
    externalZookeeperConnect = connectString;
    return self();
  }

  public PekkoConnectorsKafkaContainer withRemoteJmxService() {
    enableRemoteJmxService = true;
    return self();
  }

  public String getBootstrapServers() {
    if (port == PORT_NOT_ASSIGNED) {
      throw new IllegalStateException("You should start Kafka container first");
    }
    return "PLAINTEXT://%s:%s".formatted(getHost(), port);
  }

  public String getJmxServiceUrl() {
    if (jmxPort == PORT_NOT_ASSIGNED) {
      throw new IllegalStateException("You should start Kafka container first");
    }

    return "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi".formatted(getHost(), jmxPort);
  }

  @Override
  protected void doStart() {
    withCommand(
        "sh",
        "-c",
        "while [ ! -f " + START_STOP_SCRIPT + " ]; do sleep 0.1; done; " + START_STOP_SCRIPT);

    if (useKraft) {
      configureKraft();
    } else if (externalZookeeperConnect == null) {
      addExposedPort(ZOOKEEPER_PORT);
    }
    if (enableRemoteJmxService) {
      addExposedPort(KAFKA_JMX_PORT);
    }

    super.doStart();
  }

  private void configureKraft() {
    if (!getEnvMap().containsKey("CLUSTER_ID")) {
      withEnv("CLUSTER_ID", DEFAULT_CLUSTER_ID);
    }
    // KRaft uses node.id instead of broker.id
    getEnvMap().remove("KAFKA_BROKER_ID");
    withEnv("KAFKA_NODE_ID", brokerNum + "");
    withEnv("KAFKA_PROCESS_ROLES", "broker,controller");
    withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
    String listeners = getEnvMap().get("KAFKA_LISTENERS");
    if (listeners != null && !listeners.contains("CONTROLLER://")) {
      withEnv("KAFKA_LISTENERS", listeners + ",CONTROLLER://0.0.0.0:" + KAFKA_CONTROLLER_PORT);
    }
    String protocolMap = getEnvMap().get("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP");
    if (protocolMap != null && !protocolMap.contains("CONTROLLER:")) {
      withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", protocolMap + ",CONTROLLER:PLAINTEXT");
    }
    if (!getEnvMap().containsKey("KAFKA_CONTROLLER_QUORUM_VOTERS")) {
      // a single node quorum, KafkaContainerCluster sets the full quorum for multi-broker setups
      String host =
          getNetwork() != null
              ? getNetworkAliases().stream().findFirst().orElse("localhost")
              : "localhost";
      withEnv(
          "KAFKA_CONTROLLER_QUORUM_VOTERS", brokerNum + "@" + host + ":" + KAFKA_CONTROLLER_PORT);
    }
  }

  @Override
  protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
    try {
      super.containerIsStarting(containerInfo, reused);

      port = getMappedPort(KAFKA_PORT);
      if (enableRemoteJmxService) {
        jmxPort = getMappedPort(KAFKA_JMX_PORT);
      }

      if (reused) {
        return;
      }

      String command = "#!/bin/bash\n";
      if (!useKraft) {
        final String zookeeperConnect;
        if (externalZookeeperConnect != null) {
          zookeeperConnect = externalZookeeperConnect;
        } else {
          zookeeperConnect = "localhost:" + ZOOKEEPER_PORT;
          command += "echo 'clientPort=" + ZOOKEEPER_PORT + "' > zookeeper.properties\n";
          command += "echo 'dataDir=/var/lib/zookeeper/data' >> zookeeper.properties\n";
          command += "echo 'dataLogDir=/var/lib/zookeeper/log' >> zookeeper.properties\n";
          command += "zookeeper-server-start zookeeper.properties &\n";
        }
        command += "export KAFKA_ZOOKEEPER_CONNECT='" + zookeeperConnect + "'\n";
      }

      List<String> internalIps =
          containerInfo.getNetworkSettings().getNetworks().values().stream()
              .map(ContainerNetwork::getIpAddress)
              .toList();

      command +=
          "export KAFKA_ADVERTISED_LISTENERS='"
              + Stream.concat(
                      Stream.of(getBootstrapServers()),
                      internalIps.stream().map(ip -> "BROKER://" + ip + ":9092"))
                  .collect(Collectors.joining(","))
              + "'\n";

      if (enableRemoteJmxService) {
        String jmxIp =
            internalIps.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find IP address for JMX"));
        command += "export KAFKA_JMX_PORT='" + KAFKA_JMX_PORT + "' \n";
        command += "export KAFKA_JMX_HOSTNAME='" + jmxIp + "' \n";
      }

      command += ". /etc/confluent/docker/bash-config \n";
      command += "/etc/confluent/docker/configure \n";
      if (useKraft) {
        // formats the KRaft storage directory with the provided CLUSTER_ID (no-op when
        // already formatted, so the broker can be stopped and started again)
        command += "/etc/confluent/docker/ensure \n";
      }
      command += "/etc/confluent/docker/launch \n";

      copyFileToContainer(
          Transferable.of(command.getBytes(StandardCharsets.UTF_8), 0777), STARTER_SCRIPT);

      // start and stop the Kafka broker process without stopping the container
      String startStopWrapper =
          "#!/bin/bash\n"
              + "STARTER_SCRIPT='"
              + STARTER_SCRIPT
              + "'\n"
              + "touch /tmp/start\n"
              + "while :; do\n"
              + "\tif [ -f $STARTER_SCRIPT ]; then\n"
              + "\t\tif [ -f /tmp/stop ]; then rm /tmp/stop; /usr/bin/kafka-server-stop;\n"
              + "\t\telif [ -f /tmp/start ]; then rm /tmp/start; bash -c \"$STARTER_SCRIPT &\";fi\n"
              + "\tfi\n"
              + "\tsleep 0.1\n"
              + "done\n";

      copyFileToContainer(
          Transferable.of(startStopWrapper.getBytes(StandardCharsets.UTF_8), 0777),
          START_STOP_SCRIPT);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
