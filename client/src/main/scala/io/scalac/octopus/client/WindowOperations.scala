package io.scalac.octopus.client
import org.scalajs.dom.html.Div

import scala.scalajs.js.timers
import scalatags.JsDom.all._

/**
 * Defines basic operations on windows.
 * */
trait WindowOperations {
  protected var outsideListener: Div = _

  protected def openWindow(window: Div)(implicit octopusHome: Div) = {
    octopusHome.appendChild(window)
    outsideListener = getOutsideListener(window)(octopusHome)
    octopusHome.appendChild(outsideListener)
    timers.setTimeout(ClientConfig.WindowOpenDelay)(window.classList.remove("closed"))
  }

  protected def getOutsideListener(window: Div)(implicit octopusHome: Div): Div = div(`class` := "octopus-outside",
    onclick := { () => closeWindow(window) }
  ).render

  protected def closeWindow(window: Div)(implicit octopusHome: Div): Unit = {
    octopusHome.removeChild(outsideListener)
    window.classList.add("closed")
    timers.setTimeout(ClientConfig.WindowLoadTime)(octopusHome.removeChild(window))
  }
}
