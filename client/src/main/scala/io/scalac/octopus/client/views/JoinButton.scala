package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.octoApi
import io.scalac.octopus.client.config.Github
import io.scalac.octopus.client.views.EventWindowOperations.userId
import org.scalajs.dom
import org.scalajs.dom.html.Div

import scala.concurrent.ExecutionContext.Implicits.global
import scalac.octopusonwire.shared.domain.EventId
import scalatags.JsDom.all._

class JoinButton(window: Div, eventId: EventId) {

  /** If there's no token found, redirect to login page
    * If there's a token, just join the event
    *
    * If the event has already been joined, do nothing */
  def joinEvent(joined: Boolean) =
    if (userId.isDefined) {
      if (!joined) octoApi.joinEventAndGetJoins(eventId).call().foreach {
        eventJoinCount => {
          val bottom = window.childNodes(window.childElementCount - 2)
          bottom.replaceChild(getButton(joined = true, eventJoinCount), bottom.childNodes(0))
        }
      }
    } else dom.window.location assign Github.LoginWithJoinUrl(dom.window.location.href, eventId)

  def getButton(joined: Boolean, joinCount: Long): Div = {
    val wrapper = div(
      `class` := "octopus-user-avatar-wrapper"
    ).render

    userId.foreach(id =>
      wrapper.appendChild(
        img(
          `class` := "octopus-user-avatar",
          src := Github.buildUserAvatarUrl(id, 100)
        ).render
      )
    )
    wrapper.appendChild(
      a(
        s"${if (!joined) "Join" else "Joined"} ($joinCount)",
        `class` := "octopus-event-join-link",
        onclick := { () => joinEvent(joined) }
      ).render
    )
    wrapper
  }
}
