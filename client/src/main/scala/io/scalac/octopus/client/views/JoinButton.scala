package io.scalac.octopus.client.views

import autowire._
import io.scalac.octopus.client.config.ClientConfig.ClientApi
import io.scalac.octopus.client.config.Github
import org.scalajs.dom
import org.scalajs.dom.html.{Anchor, Div}

import scalac.octopusonwire.shared.domain.EventId
import scalatags.JsDom.all._

class JoinButton(window: Div, userLoggedIn: Boolean, eventId: EventId)(implicit api: ClientApi) {

  /** If there's no token found, redirect to login page
    * If there's a token, just join the event
    *
    * If the event has already been joined, do nothing */
  def joinEvent(joined: Boolean) =
    if (userLoggedIn) {
      if (!joined) api.joinEventAndGetJoins(eventId).call().foreach {
        eventJoinCount => {
          val bottom = window.childNodes(window.childElementCount - 2)
          bottom.replaceChild(getButton(joined = true, eventJoinCount), bottom.childNodes(0))
        }
      }
    } else dom.window.location assign Github.LoginWithJoinUrl(dom.window.location.href, eventId)

  def getButton(joined: Boolean, joinCount: Long): Anchor =
    a(
      s"${if (!joined) "Join" else "Joined"} ($joinCount)",
      `class` := "octopus-event-join-link",
      onclick := { () => joinEvent(joined) }
    ).render
}
