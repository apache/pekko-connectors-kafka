import com.typesafe.tools.mima.core.{ Problem, ProblemFilters }
import ProjectSettings.commonSettings

enablePlugins(AutomateHeaderPlugin)

ThisBuild / resolvers ++= ResolverSettings.projectResolvers

TaskKey[Unit]("verifyCodeFmt") := {
  javafmtCheckAll.all(ScopeFilter(inAnyProject)).result.value.toEither.left.foreach { _ =>
    throw new MessageOnlyException(
      "Unformatted Java code found. Please run 'javafmtAll' and commit the reformatted code")
  }
}

addCommandAlias("verifyCodeStyle", "headerCheck; verifyCodeFmt")
addCommandAlias("verifyDocs", ";+doc ;unidoc ;docs/paradoxBrowse")

lazy val `pekko-connectors-kafka` =
  project
    .in(file("."))
    .enablePlugins(ScalaUnidocPlugin)
    .disablePlugins(SitePlugin, MimaPlugin)
    .settings(commonSettings)
    .settings(
      publish / skip := true,
      // TODO: add clusterSharding to unidocProjectFilter when we drop support for Akka 2.5
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core, testkit),
      onLoadMessage := ProjectSettings.onLoadMessage)
    .aggregate(core, testkit, `cluster-sharding`, tests, benchmarks, docs)

lazy val core = project
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .settings(VersionGenerator.settings)
  .settings(MetaInfLicenseNoticeCopy.settings)
  .settings(
    name := "pekko-connectors-kafka",
    AutomaticModuleName.settings("org.apache.pekko.kafka"),
    libraryDependencies ++= Dependencies.coreDependencies,
    mimaPreviousArtifacts := Set.empty, // temporarily disable mima checks
    mimaBinaryIssueFilters += ProblemFilters.exclude[Problem]("org.apache.pekko.kafka.internal.*"))

lazy val testkit = project
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .settings(MetaInfLicenseNoticeCopy.settings)
  .settings(
    name := "pekko-connectors-kafka-testkit",
    AutomaticModuleName.settings("org.apache.pekko.kafka.testkit"),
    JupiterKeys.junitJupiterVersion := "5.8.2",
    libraryDependencies ++= Dependencies.testKitDependencies,
    libraryDependencies ++= Seq(
      "org.junit.jupiter" % "junit-jupiter-api" % JupiterKeys.junitJupiterVersion.value % Provided),
    mimaPreviousArtifacts := Set.empty, // temporarily disable mima checks
    mimaBinaryIssueFilters += ProblemFilters.exclude[Problem]("org.apache.pekko.kafka.testkit.internal.*"))

lazy val `cluster-sharding` = project
  .in(file("./cluster-sharding"))
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .settings(MetaInfLicenseNoticeCopy.settings)
  .settings(
    name := "pekko-connectors-kafka-cluster-sharding",
    AutomaticModuleName.settings("org.apache.pekko.kafka.cluster.sharding"),
    libraryDependencies ++= Dependencies.clusterShardingDependencies,
    mimaPreviousArtifacts := Set.empty // temporarily disable mima checks
  )

lazy val tests = project
  .dependsOn(core, testkit, `cluster-sharding`)
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(commonSettings)
  .settings(Defaults.itSettings)
  .settings(headerSettings(IntegrationTest))
  .settings(
    name := "pekko-connectors-kafka-tests",
    resolvers ++= ResolverSettings.testSpecificResolvers,
    libraryDependencies ++= Dependencies.testDependencies,
    libraryDependencies ++= Seq(
      "org.junit.vintage" % "junit-vintage-engine" % JupiterKeys.junitVintageVersion.value % Test,
      "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test),
    publish / skip := true,
    Test / fork := true,
    Test / parallelExecution := false,
    IntegrationTest / parallelExecution := false)

lazy val docs = project
  .enablePlugins(ParadoxPlugin, ParadoxSitePlugin, PreprocessPlugin, PublishRsyncPlugin)
  .disablePlugins(MimaPlugin)
  .settings(commonSettings)
  .settings(
    name := "Apache Pekko Kafka Connector",
    publish / skip := true,
    Compile / paradox / name := "Pekko",
    makeSite := makeSite.dependsOn(LocalRootProject / ScalaUnidoc / doc).value,
    previewPath := (Paradox / siteSubdirName).value,
    Preprocess / siteSubdirName := s"api/pekko-connectors-kafka/${projectInfoVersion.value}",
    Preprocess / sourceDirectory := (LocalRootProject / ScalaUnidoc / unidoc / target).value,
    Preprocess / preprocessRules := Seq(
      ("\\.java\\.scala".r, _ => ".java"),
      ("https://javadoc\\.io/page/".r, _ => "https://javadoc\\.io/static/"),
      // bug in Scaladoc
      ("https://docs\\.oracle\\.com/en/java/javase/11/docs/api/java.base/java/time/Duration\\$.html".r,
        _ => "https://docs\\.oracle\\.com/en/java/javase/11/docs/api/java.base/java/time/Duration.html"),
      // Add Java module name https://github.com/ThoughtWorksInc/sbt-api-mappings/issues/58
      ("https://docs\\.oracle\\.com/en/java/javase/11/docs/api/".r,
        _ => "https://docs\\.oracle\\.com/en/java/javase/11/docs/api/")),
    Paradox / siteSubdirName := s"docs/pekko-connectors-kafka/${projectInfoVersion.value}",
    ParadoxSettings.propertiesSettings,
    resolvers += Resolver.jcenterRepo,
    publishRsyncArtifacts += makeSite.value -> "www/",
    publishRsyncHost := "akkarepo@gustav.akka.io")

lazy val benchmarks = project
  .dependsOn(core, testkit)
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .configs(IntegrationTest)
  .settings(commonSettings)
  .settings(MetaInfLicenseNoticeCopy.settings)
  .settings(Defaults.itSettings)
  .settings(headerSettings(IntegrationTest))
  .settings(
    name := "pekko-connectors-kafka-benchmarks",
    publish / skip := true,
    IntegrationTest / parallelExecution := false,
    libraryDependencies ++= Dependencies.benchmarkDependencies)
