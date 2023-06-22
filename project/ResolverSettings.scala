/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt._

object ResolverSettings {
  lazy val testSpecificResolvers = Seq(
    "Confluent Maven Repo" at "https://packages.confluent.io/maven/")

  lazy val projectResolvers = Seq(
    // for Jupiter interface (JUnit 5)
    Resolver.jcenterRepo,
    // TODO: Remove when Pekko has a proper release,
    Resolver.ApacheMavenSnapshotsRepo)
}
