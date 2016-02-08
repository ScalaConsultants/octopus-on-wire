package io.scalac.octopus.client.views.detail

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.{UserThumbSize, octoApi}
import io.scalac.octopus.client.config.{ClientConfig, Github}
import io.scalac.octopus.client.views.detail.EventDetailWindow.userInfo
import org.scalajs.dom
import org.scalajs.dom.html.{Anchor, Div}
import org.scalajs.dom.location

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalac.octopusonwire.shared.domain.{EventId, EventJoinInfo, UserInfo}
import scalatags.JsDom.all._

class JoinButton(window: Div, eventId: EventId) {

  /** If there's no token found, redirect to login page
    * If there's a token, just join the event
    *
    * If the event has already been joined, do nothing */
  def joinEvent(joined: Boolean) =
    if (userInfo.isDefined) {
      if (!joined) octoApi.joinEventAndGetJoins(eventId).call().foreach {
        case EventJoinInfo(eventJoinCount, _) =>
          val left = window.firstChild
          dom.setTimeout({() =>
            left.replaceChild(getButton(joined = true, eventJoinCount, active = true), left.lastChild)
          }, 3.1 * 1000) /// consult this timeout with `octopus-rocket-flies` animation class in css
      }
    } else location assign Github.loginWithJoinUrl(dom.window.location.href, eventId)

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

  def getButton(joined: Boolean, joinCount: Long, active: Boolean): Div = {
    val rocket = img(src := "/assets/images/rocket.png", `class` := "octopus-event-join-rocket").render

    val buttonView = a(
      if (!joined) {
        rocket
      } else {
        s"\u2713 ($joinCount)"
      },
      `class` := "octopus-event-join-link",
      onclick := { () =>
        rocket.classList.add("octopus-rocket-flies")
        joinEvent(joined)
      }
    ).render

    val infoView = p(s"$joinCount joined", `class` := "octopus-event-join-count").render

    val wrapper = div(
      `class` := "octopus-user-avatar-wrapper",
      userInfo.map {
        case user if active || joined => userAvatar(user)
        case _ => "".render
      },
      if (active) buttonView else infoView
    ).render

    octoApi.getUsersJoined(eventId, ClientConfig.UsersToDisplay).call().foreach(_.foreach { person =>
      wrapper.appendChild(userAvatar(person))
    })

    wrapper
  }
}