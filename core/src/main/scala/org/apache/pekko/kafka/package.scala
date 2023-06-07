package org.apache.pekko

package object kafka {
  private[kafka] def convertPropertiesToSafeText(properties: Map[String, String]): String = {
    properties.toSeq
      .map {
        case (key, value) if value.isEmpty =>
          key -> ""
        case (key, _) =>
          key -> "*****"
      }
      .sortBy(_._1)
      .mkString(",")
  }
}
