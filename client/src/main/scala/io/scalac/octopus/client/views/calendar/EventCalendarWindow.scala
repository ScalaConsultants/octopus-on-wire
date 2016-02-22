package io.scalac.octopus.client.views.calendar

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.{ClientConfig, Github}
import io.scalac.octopus.client.views.WindowOperations
import io.scalac.octopus.client.views.addition.{DateSelector, EventCreateWindowOperations}
import io.scalac.octopus.client.views.detail.EventDetailWindow
import org.scalajs.dom
import org.scalajs.dom.html.{Anchor, Div}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.UserReputationInfo
import scalatags.JsDom.all._

object EventCalendarWindow extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None
  private var isUserSelectingDate = false

  def open(octopusHome: Div, monthDay: Date): Unit = {
    EventDetailWindow.closeWindow(octopusHome)
    EventCreateWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchWindow(octopusHome, monthDay)
  }

  def switchWindow(octopusHome: Div, current: Date): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow(octopusHome)
        None
      case None =>
        val window: Div = div(
          `class` := "octopus-window octopus-calendar closed",
          div(), //template for calendar
          div(), //template for message related to adding events
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        val calendarView = new EventCalendar(window, octopusHome)

        window.replaceChild(calendarView(current), window.firstChild)

        val addEventButton: Div = div(
          `class` := "octopus-calendar-create-event",
          "Add your own ", i(`class` := "fa fa-plus"),
          onclick := { () =>
            isUserSelectingDate = true
            window.replaceChild(new DateSelector(window, octopusHome).apply(EventCalendar.current.getOrElse(current)), window.firstChild)
            window.replaceChild(span(`class` := "select-date-prompt", "When is the event?").render, window.childNodes(1))
          }
        ).render

        val loginToAddEventButton: Anchor = a(
          `class` := "octopus-calendar-create-event",
          "Login to add events ", i(`class` := "fa fa-github"),
          href := Github.login(dom.location.href)
        ).render

        val joinEventsToAddView = (reputation: UserReputationInfo) =>
          div(
            `class` := "octopus-reinforce-copy",
            s"Hi ${reputation.userLogin}! " +
            s"To add your own events you have to build your reputation. " +
            s"To do that join ${reputation.eventAddThreshold} events via event rocket and attend them in real life. " +
            s"Your current reputation is ${reputation.userRep}. " +
            s"Keep up the good work!"
          ).render

        ClientConfig.octoApi.getUserReputation().call().foreach { result =>
          window.replaceChild(result match {
            case None => loginToAddEventButton
            case Some(rep) if rep.canAddEvents => addEventButton
            case Some(rep) => joinEventsToAddView(rep)
          }, window.childNodes(1))
        }

        openWindow(window, octopusHome)
        Option(window)
    }

  override def closeWindow(octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow, octopusHome)
      None
    case None => None
  }
}