package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.{AutowireClient, Github}
import io.scalac.octopus.client.tools.EventDateOps._
import org.scalajs.dom
import org.scalajs.dom.html.{Anchor, Div}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.UserEventInfo
import scalatags.JsDom.all._

/**
  * Manages the event detail window view.
  */
object EventWindowOperations extends WindowOperations {
  type EventWindowOption = Option[(Long, Div)]
  val api = AutowireClient[Api]


  protected var eventWindow: EventWindowOption = None
  var userLoggedIn: Boolean = false

  def openEventWindow(eventId: Long)(octopusHome: Div): Unit = {
    CalendarWindowOperations.closeWindow(octopusHome)
    eventWindow = switchEventWindow(eventId)(octopusHome)
  }

  protected def switchEventWindow(eventId: Long)(octopusHome: Div): EventWindowOption = eventWindow match {
    /*The event we want to display is the same as the one already displayed.
      Do nothing (return the same thing we matched)*/
    case Some((storedEventId, window)) if storedEventId == eventId =>
      closeWindow(octopusHome)
      None

    /*The window is visible, but the clicked event is another one.
      Close it and open a window for the clicked event*/
    case Some((_, window)) =>
      closeWindow(octopusHome)
      switchEventWindow(eventId)(octopusHome)

    /*The window is not opened. Open it.*/
    case _ =>
      api.isUserLoggedIn().call().foreach(userLoggedIn = _)

      val bottomArrow = div(`class` := "octopus-window-bottom-arrow arrow-center")
      val window = div(
        `class` := "octopus-window closed",
        span("Loading...", `class` := "octopus-loading-text"),
        bottomArrow
      ).render

      /** If there's no token found, redirect to login page
        * If there's a token, just join the event
        * If the event has already been joined, do nothing */
      def joinButton(joined: Boolean, joinCount: Long): Anchor = {
        a(
          s"${if (!joined) "Join" else "Joined"} ($joinCount)",
          `class` := "octopus-event-join-link",
          onclick := { () =>
            if (userLoggedIn) {
              if (!joined) api.joinEventAndGetJoins(eventId).call().foreach {
                eventJoinCount => {
                  val bottom = window.childNodes(window.childElementCount - 2)
                  bottom.replaceChild(joinButton(joined = true, eventJoinCount), bottom.childNodes(0))
                }
              }
            }
            else
              dom.window.location assign Github.LoginWithJoinUrl(dom.window.location.href, eventId)
          }).render
      }

      api.getUserEventInfo(eventId).call().foreach {
        case UserEventInfo(Some(event), joined, joinCount) =>

          //clear window
          while (window.childElementCount > 0)
            window.removeChild(window.firstChild)

          Array(
            h1(event.name, `class` := "octopus-event-name"),
            p(event.datesToString, `class` := "octopus-event-date"),
            p(event.location, `class` := "octopus-event-location"),
            div(
              `class` := "octopus-event-bottom",
              joinButton(joined, joinCount),
              a(href := event.url, `class` := "octopus-event-link", target := "_blank")
            ),
            bottomArrow
          ).foreach(a => window.appendChild(a.render))
        case _ => /* no such event */
      }

      openWindow(window)(octopusHome)
      Option((eventId, window))
  }

  override def closeWindow(octopusHome: Div): Unit = eventWindow = eventWindow match {
    case Some((_, openedWindow)) =>
      removeWindow(openedWindow)(octopusHome)
      None
    case None => None
  }
}

