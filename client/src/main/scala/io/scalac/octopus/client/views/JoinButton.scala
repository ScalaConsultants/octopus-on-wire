package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.{UserThumbSize, octoApi}
import io.scalac.octopus.client.config.{ClientConfig, Github}
import io.scalac.octopus.client.views.EventWindowOperations.userInfo
import org.scalajs.dom
import org.scalajs.dom.html.{Anchor, Div}

import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalac.octopusonwire.shared.domain.{EventId, UserInfo}
import scalatags.JsDom.all._

class JoinButton(window: Div, eventId: EventId) {

  /** If there's no token found, redirect to login page
    * If there's a token, just join the event
    *
    * If the event has already been joined, do nothing */
  def joinEvent(joined: Boolean) =
    if (userInfo.isDefined) {
      if (!joined) octoApi.joinEventAndGetJoins(eventId).call().foreach {
        eventJoinCount => {
          val bottom = window.childNodes(window.childElementCount - 2)
          bottom.replaceChild(getButton(joined = true, eventJoinCount), bottom.childNodes(0))
        }
      }
    } else dom.window.location assign Github.LoginWithJoinUrl(dom.window.location.href, eventId)

  def userAvatar(user: UserInfo): Anchor = a(
    `class` := "octopus-user-avatar",
    href := Github.buildUserPageUrl(user.login),
    target := "_blank",
    img(src := Github.buildUserAvatarUrl(user.userId, UserThumbSize)),
    span(
      `class` := "octopus-user-nickname",
      user.login
    )
  ).render

  def getButton(joined: Boolean, joinCount: Long): Div = {
    val wrapper = div(
      `class` := "octopus-user-avatar-wrapper"
    ).render

    userInfo.foreach(user =>
      wrapper.appendChild(userAvatar(user))
    )

    wrapper.appendChild(
      a(
        s"${if (!joined) "+=1" else "\u2713"} ($joinCount)",
        `class` := "octopus-event-join-link",
        onclick := { () => joinEvent(joined) }
      ).render
    )

    octoApi.getUsersJoined(eventId, ClientConfig.UsersToDisplay).call().foreach(_.foreach { person =>
      wrapper.appendChild(userAvatar(person))
    })

    wrapper
  }
}
