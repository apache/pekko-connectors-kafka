---
project.description: Apache Pekko Discovery can be used to achieve Kafka broker discovery from the operations environment.
---
# Service discovery

By using @extref:[Pekko Discovery](pekko:discovery/index.html) Apache Pekko Connectors Kafka may read the Kafka bootstrap server addresses from any Apache Pekko Discovery-compatible service discovery mechanism.

Apache Pekko Discovery supports Configuration (HOCON), DNS (SRV records), and aggregation of multiple discovery methods out-of-the-box. Kubernetes API, AWS API: EC2 Tag-Based Discovery, AWS API: ECS Discovery and Consul implementations for Apache Pekko Discovery are available in @extref:[Pekko Management](pekko-management:).

## Dependency

The Apache Pekko Discovery version must match the Apache Pekko version used in your build. To use the implementations provided by Apache Pekko Management, you need to add the desired dependency.

@@dependency [Maven,sbt,Gradle] {
  symbol=PekkoVersion
  value=$pekko.version$
  group=org.apache.pekko
  artifact=pekko-discovery_$scala.binary.version$
  version=PekkoVersion
}

## Configure consumer settings

To use Apache Pekko Discovery with Apache Pekko Connectors Kafka consumers, configure a section for your consumer settings which inherits the default settings (by using `${pekko.kafka.consumer}`) and add a service name and a timeout for the service lookup. Setting the `service-name` in the `pekko.kafka.consumer` config will work, if all your consumers connect to the same Kafka broker.

The service name must match the one configured with the discovery technology you use. Overwrite the `resolve-timeout` depending on the discovery technology used, and your environment.

Note that consumers and producers may share a service (as shown in the examples on this page).

application.conf
:   ```hocon
    discovery-consumer: ${pekko.kafka.consumer} {
      service-name = "kafkaService1"
    }
    ```

Mount the @apidoc[DiscoverySupport$] in your consumer settings:

Scala
: @@snip [snip](/tests/src/test/scala/org/apache/pekko/kafka/ConsumerSettingsSpec.scala) { #discovery-settings }

Java
: @@snip [conf](/tests/src/test/java/docs/javadsl/ConsumerSettingsTest.java) { #discovery-settings }


## Configure producer settings

To use Apache Pekko Discovery with Apache Pekko Connectors Kafka producers, configure a section for your producer settings which inherits the default settings (by using `${pekko.kafka.producer}`) and add a service name and a timeout for the service lookup. Setting the `service-name` in the `pekko.kafka.producer` config will work, if all your producers connect to the same Kafka broker.

The service name must match the one configured with the discovery technology you use. Overwrite the `resolve-timeout` depending on the discovery technology used, and your environment.

Note that consumers and producers may share a service (as shown in the examples on this page).

application.conf
:   ```hocon
    discovery-producer: ${pekko.kafka.producer} {
      service-name = "kafkaService1"
    }
    ```

Mount the @apidoc[DiscoverySupport$] in your producer settings:

Scala
: @@snip [conf](/tests/src/test/scala/org/apache/pekko/kafka/ProducerSettingsSpec.scala) { #discovery-settings }

Java
: @@snip [conf](/tests/src/test/java/docs/javadsl/ProducerSettingsTest.java) { #discovery-settings }


## Provide a service name via environment variables

To set the service name for lookup of the Kafka brokers bootstrap addresses via environment variables, use the built-in s support in Typesafe Config as below. This example will use the value from the environment variable `KAFKA_SERVICE_NAME` and in case that is not defined default to `kafkaServiceDefault`.

application.conf
:   &#9;

    ```hocon
    pekko.kafka.producer {
      service-name = "kafkaServiceDefault"
      service-name = ${?KAFKA_SERVICE_NAME}
    }
    
    pekko.kafka.consumer {
      service-name = "kafkaServiceDefault"
      service-name = ${?KAFKA_SERVICE_NAME}
    }
    ```



## Specify a different service discovery mechanism

The Actor System-wide service discovery is used by default, to choose a different Apache Pekko Discovery implementation, set the `discovery-method` setting in the producer and consumer configurations accordingly.

application.conf
:   ```hocon
    discovery-producer: ${pekko.kafka.producer} {
      discovery-method = "kubernetes-api"
      service-name = "kafkaService1"
      resolve-timeout = 3 seconds
    }
    ```

## Use Config (HOCON) to describe the bootstrap servers

The setup below uses the built-in Apache Pekko Discovery implementation reading from Config (HOCON) files. That might be a good choice for development and testing. You may use the @extref:[Aggregate implementation](pekko:discovery/index.html#discovery-method-aggregate-multiple-discovery-methods) to first use another discovery technology, before falling back to the config file.

application.conf
:   @@snip [conf](/tests/src/test/scala/org/apache/pekko/kafka/ConsumerSettingsSpec.scala) { #discovery-with-config }
