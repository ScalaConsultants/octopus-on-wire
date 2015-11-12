package tools

import play.api.libs.json._

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

  def toOptionSeq: Option[Seq[JsValue]] = jsLookupResult match{
    case JsDefined(JsArray(vals)) => Option(vals)
    case _ => None
  }
}

object JsLookupResultOps {
  implicit def JsLookupResult2Ops(lookupResult: JsLookupResult): JsLookupResultOps = new JsLookupResultOps(lookupResult)
}