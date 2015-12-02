package io.scalac.octopus.client.views.addition

import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.KeyCheckDelay
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{FocusEvent, HTMLElement}

import scala.language.postfixOps
import scala.scalajs.js.timers
import scala.util.{Success, Try}
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scalatags.JsDom.all._

class IntOnlyKeyHandler(maxLength: Int, min: Int, max: Int, view: HTMLElement, validationCallback: () => Unit) {
  var lastValidValue = view.textContent

  def handleValidKey(ch: Char): Unit =
    timers.setTimeout(KeyCheckDelay) {
      if (view.textContent.length > maxLength || !(view.textContent.toInt inRange(min, max)))
        view.textContent = lastValidValue
      else
        lastValidValue = view.textContent
      validationCallback()
    }

  def handleEvent(e: KeyboardEvent) = {
    val ch = e.charCode.toChar
    Try(ch.toString.toInt) match {
      case Success(_) => handleValidKey(ch)
      // case Failure(_) if List('-', '+') contains ch => handleValidKey(ch)
      case _ =>
        //called here because the first case runs a delayed operation
        validationCallback()
        e.preventDefault()
    }
  }
}

object IntOnlyKeyHandler{
  def getNewTimeField(hint: String, min: Int, max: Int, maxLength: Int,
                      validationCallback: () => Unit): Span = {
    val field = span(placeholder := hint, contenteditable := "true").render
    val handler = new IntOnlyKeyHandler(maxLength, min, max, field, validationCallback)
    field.onkeypress = (event: KeyboardEvent) => {
      handler.handleEvent(event)
    }
    field.onblur = (event: FocusEvent) => {
      validationCallback()
    }
    field
  }
}