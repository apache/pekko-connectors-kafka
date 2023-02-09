# Debugging

Debugging setups with the Apache Pekko Connectors Kafka Connector will be required at times. This page collects a few ideas to start out with in case the connector does not behave as you expected.

## Logging with SLF4J

Apache Pekko, Apache Pekko Streams and thus the Apache Pekko Connectors Kafka Connector support [SLF4J logging API](https://www.slf4j.org/) by adding Apache Pekko's SLF4J module and an SLF4J compatible logging framework, eg. [Logback](https://logback.qos.ch/).

The Kafka client library used by the Apache Pekko Connectors Kafka connector uses SLF4J, as well.

@@dependency [Maven,sbt,Gradle] {
  symbol=PekkoVersion
  value="$akka.version$"
  group=org.apache.pekko
  artifact=akka-slf4j_$scala.binary.version$
  version=PekkoVersion
  group2=ch.qos.logback
  artifact2=logback-classic
  version2=1.2.3
}

To enable Apache Pekko SLF4J logging, configure Apache Pekko in `application.conf` as below. Refer to the @extref[Pekko documentation](pekko:logging.html#slf4j) for details.

```hocon
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "DEBUG"
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
}
```

## Receive logging

In case you're debugging the internals in the Kafka Consumer actor, you might want to enable receive logging to see all messages it receives. To lower the log message volume, change the Kafka poll interval to something larger, eg. 300 ms.

```hocon
akka {
  actor {
    debug.receive = true
  }
  kafka.consumer {
    poll-interval = 300ms
  }
}
```
