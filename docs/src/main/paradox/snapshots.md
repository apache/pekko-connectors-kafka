---
project.description: Snapshot builds of Apache Pekko Connectors Kafka are provided via the Apache snapshot repository.
---
# Snapshots

[snapshots]:        https://repository.apache.org/content/groups/snapshots/org/apache/pekko/pekko-connectors-kafka_2.13/

Snapshots are published to the Apache's Snapshot repository every night.

**Please do not use snapshots for anything other than testing.**

Add the following to your project build definition to resolve Apache Pekko Connectors Kafka connector snapshots:

## Configure repository

Maven
:   ```xml
    <project>
    ...
      <repositories>
        <repository>
            <id>snapshots-repo</id>
            <name>Apache snapshots</name>
            <url>https://repository.apache.org/content/groups/snapshots</url>
        </repository>
      </repositories>
    ...
    </project>
    ```

sbt
:   ```scala
    resolvers += "Apache Snapshots" at "https://repository.apache.org/content/groups/snapshots"
    ```

Gradle
:   ```gradle
    repositories {
      maven {
        url  "https://repository.apache.org/content/groups/snapshots"
      }
    }
    ```

## Documentation

The [snapshot documentation](https://pekko.apache.org/docs/pekko-connectors-kafka/snapshot/) is updated with every snapshot build.

## Versions

The snapshot repository is cleaned from time to time with no further notice. Check [Apache Snapshots files](https://repository.apache.org/content/groups/snapshots/org/apache/pekko/pekko-connectors-kafka_2.13/) to see what versions are currently available.
