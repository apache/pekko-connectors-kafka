import sbt._
import sbt.Keys._

/**
 * Generate version.conf and pekko/kafka/Version.scala files based on the version setting.
 *
 * This was adapted from https://github.com/apache/incubator-pekko/blob/main/project/VersionGenerator.scala
 */
object VersionGenerator {

  val settings: Seq[Setting[_]] = inConfig(Compile)(
    Seq(
      resourceGenerators += generateVersion(resourceManaged, _ / "version.conf", """|pekko.kafka.version = "%s"
         |"""),
      sourceGenerators += generateVersion(
        sourceManaged,
        _ / "pekko" / "kafka" / "Version.scala",
        """|package org.apache.pekko.kafka
         |
         |object Version {
         |  val current: String = "%s"
         |}
         |""")))

  def generateVersion(dir: SettingKey[File], locate: File => File, template: String) = Def.task[Seq[File]] {
    val file = locate(dir.value)
    val content = template.stripMargin.format(version.value)
    if (!file.exists || IO.read(file) != content) IO.write(file, content)
    Seq(file)
  }

}
