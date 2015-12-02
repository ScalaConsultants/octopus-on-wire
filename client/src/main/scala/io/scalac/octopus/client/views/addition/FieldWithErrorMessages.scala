package io.scalac.octopus.client.views.addition

import io.scalac.octopus.client.config.ClientConfig._
import org.scalajs.dom.raw.{UIEvent, HTMLElement}

import scala.scalajs.js.timers
import scalatags.JsDom.all._

class FieldWithErrorMessages[T <: HTMLElement] private(field: T, messages: Set[String]) {
  def updateErrors(errors: Set[String]) = {
    messages.zipWithIndex.partition(errors contains _._1) match {
      case (invalid, valid) =>
        invalid.foreach { case (_, index) => contents.children(1 + index).classList add "invalid" }
        valid foreach { case (_, index) => contents.children(1 + index).classList remove "invalid" }
    }
  }

  def getText = field.textContent

  /*Adds "dirty" class - fields without it won't show any errors.
  * Activated by typing in/focusing out of a field, or submitting the form. */
  def makeDirty() = contents.classList add "dirty"

  private val wrapperClass = Option(field.className).filterNot(_ == "").map(" " + _ + "-wrapper")

  val contents = div(
    `class` := "octopus-validation-parent" + wrapperClass.getOrElse(""),
    field,
    messages.map { message =>
      span(`class` := "octopus-validation-message", "!", title := message).render
    }.toList
  ).render
}


object FieldWithErrorMessages {
  def apply[T <: HTMLElement](field: T, messageSource: Map[String, HTMLElement]) =
    new FieldWithErrorMessages[T](field, messageSource.filter(_._2 == field).keySet)

  //adds keypress / focusleave listeners that call specified callback
  def addValidationListeners[T <: HTMLElement](field: T, callback: () => Unit): T = {
    val listener = (_: UIEvent) => {
      field.parentElement.classList add "dirty"
      timers.setTimeout(KeyCheckDelay)(callback())
    }

    field.onkeypress = listener
    field.onblur = listener

    field
  }
}
