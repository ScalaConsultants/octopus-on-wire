package io.scalac.octopus.client.tools

import scala.language.postfixOps

object FindSuffixInMessage{
  def apply(message: String, suffixes: Seq[String]): Option[String] = suffixes find (message endsWith)
}