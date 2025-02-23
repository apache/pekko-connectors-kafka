/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import net.bzzt.reproduciblebuilds.ReproducibleBuildsPlugin.reproducibleBuildsCheckResolver
import com.github.pjfanning.pekkobuild._
import com.typesafe.tools.mima.core.{ Problem, ProblemFilters }
import ProjectSettings.commonSettings

sourceDistName := "apache-pekko-connectors-kafka"
sourceDistIncubating := false

ThisBuild / reproducibleBuildsCheckResolver := Resolver.ApacheMavenStagingRepo

addCommandAlias("verifyCodeStyle", "scalafmtCheckAll; scalafmtSbtCheck; +headerCheckAll; javafmtCheckAll")
addCommandAlias("applyCodeStyle", "+headerCreateAll; scalafmtAll; scalafmtSbt; javafmtAll")

addCommandAlias("verifyDocs", ";+doc ;unidoc ;docs/paradoxBrowse")

val mimaCompareVersion = "1.0.0"

lazy val `pekko-connectors-kafka` =
  project
    .in(file("."))
    .enablePlugins(ScalaUnidocPlugin)
    .disablePlugins(SitePlugin, MimaPlugin)
    .settings(commonSettings)
    .settings(
      name := "pekko-connectors-kafka-root",
      publish / skip := true,
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core, testkit, `cluster-sharding`),
      onLoadMessage := ProjectSettings.onLoadMessage)
    .aggregate(core, testkit, `cluster-sharding`, tests, `java-tests`, benchmarks, docs)

lazy val core = project
  .enablePlugins(ReproducibleBuildsPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .settings(VersionGenerator.settings)
  .addPekkoModuleDependency("pekko-stream", "", PekkoCoreDependency.default)
  .addPekkoModuleDependency("pekko-discovery", "provided", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka",
    AutomaticModuleName.settings("org.apache.pekko.kafka"),
    libraryDependencies ++= Dependencies.coreDependencies,
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % mimaCompareVersion),
    mimaBinaryIssueFilters += ProblemFilters.exclude[Problem]("org.apache.pekko.kafka.internal.*"))

lazy val testkit = project
  .dependsOn(core)
  .enablePlugins(ReproducibleBuildsPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .addPekkoModuleDependency("pekko-stream-testkit", "", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka-testkit",
    AutomaticModuleName.settings("org.apache.pekko.kafka.testkit"),
    JupiterKeys.junitJupiterVersion := "5.12.0",
    libraryDependencies ++= Dependencies.testKitDependencies,
    libraryDependencies ++= Seq(
      "org.junit.jupiter" % "junit-jupiter-api" % JupiterKeys.junitJupiterVersion.value % Provided),
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % mimaCompareVersion),
    mimaBinaryIssueFilters += ProblemFilters.exclude[Problem]("org.apache.pekko.kafka.testkit.internal.*"))

lazy val `cluster-sharding` = project
  .in(file("./cluster-sharding"))
  .dependsOn(core)
  .enablePlugins(ReproducibleBuildsPlugin)
  .disablePlugins(SitePlugin)
  .settings(commonSettings)
  .addPekkoModuleDependency("pekko-cluster-sharding-typed", "", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka-cluster-sharding",
    AutomaticModuleName.settings("org.apache.pekko.kafka.cluster.sharding"),
    AddMetaInfLicenseFiles.clusterShardingSettings,
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % mimaCompareVersion))

lazy val tests = project
  .dependsOn(core, testkit, `cluster-sharding`)
  .disablePlugins(MimaPlugin, SitePlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings))
  .settings(commonSettings)
  .settings(Defaults.itSettings)
  .settings(headerSettings(IntegrationTest))
  .addPekkoModuleDependency("pekko-discovery", "test", PekkoCoreDependency.default)
  .addPekkoModuleDependency("pekko-slf4j", "test", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka-tests",
    resolvers ++= ResolverSettings.testSpecificResolvers,
    libraryDependencies ++= Dependencies.testDependencies,
    publish / skip := true,
    Test / fork := true,
    Test / parallelExecution := false,
    IntegrationTest / parallelExecution := false)

lazy val `java-tests` = project
  .dependsOn(core, testkit, `cluster-sharding`, tests % "compile->compile;test->test")
  .disablePlugins(MimaPlugin, SitePlugin)
  .settings(commonSettings)
  .addPekkoModuleDependency("pekko-discovery", "test", PekkoCoreDependency.default)
  .addPekkoModuleDependency("pekko-slf4j", "test", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka-java-tests",
    resolvers ++= ResolverSettings.testSpecificResolvers,
    libraryDependencies ++= Dependencies.testDependencies,
    libraryDependencies ++= Seq(
      "org.junit.vintage" % "junit-vintage-engine" % JupiterKeys.junitVintageVersion.value % Test,
      "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test),
    publish / skip := true,
    Test / compileOrder := CompileOrder.ScalaThenJava,
    Test / fork := true,
    Test / parallelExecution := false,
    IntegrationTest / parallelExecution := false)

lazy val docs = project
  .enablePlugins(ParadoxPlugin, PekkoParadoxPlugin, ParadoxSitePlugin, PreprocessPlugin)
  .disablePlugins(MimaPlugin)
  .settings(commonSettings)
  .settings(
    name := "Apache Pekko Kafka Connector",
    publish / skip := true,
    Compile / paradox / name := "Pekko",
    makeSite := makeSite.dependsOn(LocalRootProject / ScalaUnidoc / doc).value,
    previewPath := (Paradox / siteSubdirName).value,
    Global / pekkoParadoxIncubatorNotice := None,
    Preprocess / siteSubdirName := s"api/pekko-connectors-kafka/${projectInfoVersion.value}",
    Preprocess / sourceDirectory := (LocalRootProject / ScalaUnidoc / unidoc / target).value,
    Preprocess / preprocessRules := Seq(
      ("\\.java\\.scala".r, _ => ".java"), ("https://javadoc\\.io/page/".r, _ => "https://javadoc\\.io/static/"),
      // bug in Scaladoc
      ("https://docs\\.oracle\\.com/en/java/javase/11/docs/api/java.base/java/time/Duration\\$.html".r,
        _ => "https://docs\\.oracle\\.com/en/java/javase/11/docs/api/java.base/java/time/Duration.html"),
      // Add Java module name https://github.com/ThoughtWorksInc/sbt-api-mappings/issues/58
      ("https://docs\\.oracle\\.com/en/java/javase/11/docs/api/".r,
        _ => "https://docs\\.oracle\\.com/en/java/javase/11/docs/api/")),
    Paradox / siteSubdirName := s"docs/pekko-connectors-kafka/${projectInfoVersion.value}",
    ParadoxSettings.settings)

lazy val benchmarks = project
  .dependsOn(core, testkit)
  .disablePlugins(MimaPlugin, SitePlugin)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings))
  .settings(commonSettings)
  .settings(Defaults.itSettings)
  .settings(headerSettings(IntegrationTest))
  .addPekkoModuleDependency("pekko-slf4j", "it", PekkoCoreDependency.default)
  .addPekkoModuleDependency("pekko-stream-testkit", "it", PekkoCoreDependency.default)
  .settings(
    name := "pekko-connectors-kafka-benchmarks",
    publish / skip := true,
    IntegrationTest / parallelExecution := false,
    libraryDependencies ++= Dependencies.benchmarkDependencies)
