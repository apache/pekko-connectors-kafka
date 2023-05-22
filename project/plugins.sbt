addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.1")
addSbtPlugin("org.mdedetrich" % "sbt-apache-sonatype" % "0.1.7")
addSbtPlugin("com.github.pjfanning" % "sbt-source-dist" % "0.1.5")
// discipline
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")
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

addSbtPlugin("org.apache.pekko" % "pekko-sbt-paradox" % "0.0.0+38-68da3106-SNAPSHOT")
