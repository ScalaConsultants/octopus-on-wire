package io.scalac.octopus.client.tools

import scala.language.postfixOps

case class FindSuffixInMessage(message: String, suffixes: Seq[String])

object SuffixFound {
  def unapply(query: FindSuffixInMessage): Option[String] = query.suffixes find (query.message endsWith)
}
