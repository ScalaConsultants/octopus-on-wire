package tools

import play.api.libs.json.{JsNumber, JsString, JsDefined, JsLookupResult}

class JsLookupResultOps(jsLookupResult: JsLookupResult) {
  def toOptionString: Option[String] = jsLookupResult match {
    case JsDefined(JsString(str)) => Option(str)
    case _ => None
  }

  def toOptionLong: Option[Long] = jsLookupResult match {
    case JsDefined(JsNumber(num)) => Option(num.toLong)
  }
}

object JsLookupResultOps {
  implicit def JsLookupResult2Ops(lookupResult: JsLookupResult): JsLookupResultOps = new JsLookupResultOps(lookupResult)
}