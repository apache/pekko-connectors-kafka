import sbt._

object ResolverSettings {
  lazy val testSpecificResolvers = Seq(
    "Confluent Maven Repo" at "https://packages.confluent.io/maven/")

  lazy val projectResolvers = Seq(
    // for Jupiter interface (JUnit 5)
    Resolver.jcenterRepo,
    "Apache Snapshot Repo" at "https://repository.apache.org/content/groups/snapshots/")
}
