package io.scalac.octopus.client.views.addition

import boopickle.Default._
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scalac.octopusonwire.shared.domain.Event._

trait ErrorMessages {
  self: EventAdditionForm =>

  val MalformedDatesMessage = "We need to know when the event starts and ends!"

  //error messages with their appropriate fields
  val messagesWithFields: Map[String, HTMLElement] = Map(
    MalformedDatesMessage -> endMinuteField,
    InvalidNameMessage -> eventNameField,
    InvalidDatesMessage -> endMinuteField,
    //    InvalidOffsetMessage -> timezoneMinuteField,
    InvalidLocationMessage -> eventLocationField,
    InvalidURLMessage -> eventUrlField
  )

  //validated fields wrappers
  val eventNameWrapper = FieldWithErrorMessages(eventNameField, messagesWithFields)
  val eventLocationWrapper = FieldWithErrorMessages(eventLocationField, messagesWithFields)
  val eventUrlWrapper = FieldWithErrorMessages(eventUrlField, messagesWithFields)
  val endMinuteWrapper = FieldWithErrorMessages(endMinuteField, messagesWithFields)

  //and a list of them all
  val fieldsWithValidation = List(eventNameWrapper, eventLocationWrapper, eventUrlWrapper, endMinuteWrapper)


  def validateForm() =
    buildEventOrGetErrors match {
      case Right(errors) => showErrors(errors)
      case _ => showErrors(Set.empty)
    }

  /* used with time fields that don't have their own FieldWithError instance.
   they call endMinuteField's instead */
  def validateFormAndCheckTimeErrors(): Unit = {
    endMinuteWrapper.makeDirty()
    validateForm()
  }

  def showErrors(errors: Set[String]): Unit =
    fieldsWithValidation.foreach(_.updateErrors(errors))
}
