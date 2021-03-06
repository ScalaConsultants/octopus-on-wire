package io.scalac.octopus.client.views.detail

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.OctopusClient
import io.scalac.octopus.client.config.ClientConfig.{TwitterSharingText, octoApi}
import io.scalac.octopus.client.config.{ClientConfig, Github}
import io.scalac.octopus.client.tools.EncodableString.string2Encodable
import io.scalac.octopus.client.tools.EventDateOps._
import io.scalac.octopus.client.tools.{ClickEvent, UserEvent}
import io.scalac.octopus.client.views.addition.EventCreateWindowOperations
import io.scalac.octopus.client.views.calendar.EventCalendarWindow
import io.scalac.octopus.client.views.{SliderViewOperations, WindowOperations}
import org.scalajs.dom.html.{Anchor, Div}
import org.scalajs.dom.window.location
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers
import scalac.octopusonwire.shared.domain.{Event, EventId, UserEventInfo, UserInfo}
import scalatags.JsDom.all._

/**
  * Manages the event detail window view.
  */
object EventDetailWindow extends WindowOperations {
  type EventWindowOption = Option[(EventId, Div)]

  var userInfo: Option[UserInfo] = None

  octoApi.getUserInfo().call().map(userInfo = _).recover{
    case _ => println("Invalid GitHub token")
  }

  protected var eventWindow: EventWindowOption = None

  def open(eventId: EventId, octopusHome: Div, eventType: UserEvent): Unit = {
    EventCalendarWindow.closeWindow(octopusHome)
    EventCreateWindowOperations.closeWindow(octopusHome)
    eventWindow = switchWindow(eventId, octopusHome, eventType)
  }

  protected def switchWindow(eventId: EventId, octopusHome: Div, eventType: UserEvent): EventWindowOption = eventWindow match {
    /*The event we want to display is the same as the one already displayed. */
    case Some((storedEventId, window)) if storedEventId == eventId =>
      eventWindow // do nothing, user will close either by clicking outside of detail or using X

    /*The window is visible, but the clicked event is another one.
      Close it and open a window for the clicked event*/
    case Some((_, window)) =>
      if (eventType == ClickEvent) {
        // prevent from accidental hovers damaging
        closeWindow(octopusHome)
        switchWindow(eventId, octopusHome, eventType)
      } else eventWindow

    /*The window is closed. Open it.*/
    case _ =>
      var flagging = false

      val bottomArrow = div(`class` := "octopus-window-bottom-arrow arrow-center")
      val window = div(
        `class` := "octopus-window octopus-event-view closed",
        span("Loading...", `class` := "octopus-loading-text"),
        bottomArrow
      ).render

      def flagEvent() = {
        flagging = true
        octoApi.flagEvent(eventId).call().foreach { _ =>
          closeWindow(octopusHome)
          timers.setTimeout(ClientConfig.WindowLoadTime) {
            OctopusClient.refreshEvents(SliderViewOperations.list)
          }
        }
      }

      def flagIcon(): Anchor = {
        val canFlag = userInfo.isDefined && !flagging
        a(
          `class` := "octopus-link octopus-event-flag",
          onclick := {
            () => canFlag match {
              case true => flagEvent()
              case _ if userInfo.isEmpty =>
                location assign Github.loginWithFlagUrl(location.href, eventId)
            }
          },
          title := (canFlag match {
            case true => "Click to report and hide the event"
            case _ if flagging => "Flagging event..."
            case _ => "Please login to flag event"
          })
        ).render
      }

      val elemArrayFromUserEventInfo: UserEventInfo => Array[HTMLElement] = {
        case UserEventInfo(event, joined, joinCount, eventActive) =>
          Array(
            div(
              `class` := "octopus-event view-left",
              h1(event.name, `class` := "octopus-event-name"),
              p(event.datesToString, `class` := "octopus-event-date"),
              p(event.location, `class` := "octopus-event-location"),
              new JoinButton(window, eventId)
                .getButton(joined, joinCount, eventActive)
            ),
            div(`class` := "octopus-event view-right",
              closingXIcon(octopusHome),
              twitterLinkIcon(event),
              a(href := event.url, `class` := "octopus-link octopus-event-link", target := "_blank"),
              flagIcon()
            ),
            bottomArrow
          ).map(_.render)
      }

      octoApi.getUserEventInfo(eventId).call().map {
        case Some(info) =>
          //clear window
          while (window.childElementCount > 0)
            window.removeChild(window.firstChild)

          elemArrayFromUserEventInfo(info).foreach(window appendChild _)

        case _ => println("No event found for id: " + eventId)
      }

      openWindow(window, octopusHome)
      Option((eventId, window))
  }

  def closingXIcon(octopusHome: Div): Anchor = a(
    `class` := "octopus-link octopus-event-close",
    onclick := { () =>
      closeWindow(octopusHome)
    }
  ).render

  def twitterLinkIcon(event: Event): Anchor = {
    a(
      href := s"https://twitter.com/intent/tweet?text=${TwitterSharingText.format(event.name).encode}&url=${event.url.encode}",
      `class` := "octopus-link octopus-twitter-link", target := "_blank"
    ).render
  }

  override def closeWindow(octopusHome: Div): Unit = eventWindow = eventWindow match {
    case Some((_, openedWindow)) =>
      removeWindow(openedWindow, octopusHome)
      None
    case None => None
  }
}

