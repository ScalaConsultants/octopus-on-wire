package scalac.octopusonwire.shared.tools
import scala.languageFeature.implicitConversions

class LongRangeOps(i: Long){
  def inRange(a: Long, b: Long) = i >= a && i <= b
}

object LongRangeOps{
  implicit def int2IntRangeOps(i: Int): LongRangeOps = new LongRangeOps(i)
  implicit def long2IntRangeOps(l: Long): LongRangeOps = new LongRangeOps(l)
}
