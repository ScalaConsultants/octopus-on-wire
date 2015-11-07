package tools

import play.api.libs.json.{JsDefined, JsLookupResult, JsNumber, JsString}

import scala.language.implicitConversions

class JsLookupResultOps(jsLookupResult: JsLookupResult) {
  def toOptionString: Option[String] = jsLookupResult match {
    case JsDefined(JsString(str)) => Option(str)
    case _ => None
  }

  def toOptionLong: Option[Long] = jsLookupResult match {
    case JsDefined(JsNumber(num)) => Option(num.toLong)
    case _ => None
  }
}

object JsLookupResultOps {
  implicit def JsLookupResult2Ops(lookupResult: JsLookupResult): JsLookupResultOps = new JsLookupResultOps(lookupResult)
}