/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("net.bzzt" % "sbt-reproducible-builds" % "0.30")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.1")
addSbtPlugin("org.mdedetrich" % "sbt-apache-sonatype" % "0.1.10")
addSbtPlugin("com.github.pjfanning" % "sbt-source-dist" % "0.1.8")
addSbtPlugin("com.github.sbt" % "sbt-license-report" % "1.6.1")
// discipline
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.9.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.7.0")
// docs
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
// Java 11 module names are not added https://github.com/ThoughtWorksInc/sbt-api-mappings/issues/58
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "3.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")

resolvers += Resolver.jcenterRepo
// allow access to snapshots for pekko-sbt-paradox
resolvers += Resolver.ApacheMavenSnapshotsRepo
updateOptions := updateOptions.value.withLatestSnapshots(false)

// We have to deliberately use older versions of sbt-paradox because current Pekko sbt build
// only loads on JDK 1.8 so we need to bring in older versions of parboiled which support JDK 1.8
addSbtPlugin(("org.apache.pekko" % "pekko-sbt-paradox" % "0.0.0+56-bff08336-SNAPSHOT").excludeAll(
  "com.lightbend.paradox", "sbt-paradox",
  "com.lightbend.paradox" % "sbt-paradox-apidoc",
  "com.lightbend.paradox" % "sbt-paradox-project-info"))
addSbtPlugin(("com.lightbend.paradox" % "sbt-paradox" % "0.9.2").force())
addSbtPlugin(("com.lightbend.paradox" % "sbt-paradox-apidoc" % "0.10.1").force())
addSbtPlugin(("com.lightbend.paradox" % "sbt-paradox-project-info" % "2.0.0").force())
