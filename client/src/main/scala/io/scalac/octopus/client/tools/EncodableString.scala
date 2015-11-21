package io.scalac.octopus.client.tools

import scala.language.implicitConversions

class EncodableString(string: String) {
  def encode = scalajs.js.URIUtils.encodeURIComponent(string)
}

object EncodableString{
  implicit def string2Encodable(s: String): EncodableString = new EncodableString(s)
}