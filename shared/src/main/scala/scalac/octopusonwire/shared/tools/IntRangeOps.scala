package scalac.octopusonwire.shared.tools
import scala.languageFeature.implicitConversions

class IntRangeOps(i: Int){
  def inRange(a: Int, b: Int) = i >= a && i <= b
}

object IntRangeOps{
  implicit def int2IntRangeOps(i: Int): IntRangeOps = new IntRangeOps(i)
}
