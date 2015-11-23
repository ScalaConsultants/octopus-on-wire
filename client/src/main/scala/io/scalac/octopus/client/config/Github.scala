package io.scalac.octopus.client.config

import scalac.octopusonwire.shared.domain.{UserId, EventId}
import ClientConfig.ApiUrl

object Github {
  val RootPath = "https://github.com"

  val AuthorizeUrl = s"$RootPath/login/oauth/authorize"

  val ClientId = "23d0d2f1f1cad53fd5f8"
  val LoginUrl = s"$AuthorizeUrl?client_id=$ClientId"

  private def loginWithRedirectUrl(redirectTo: String, currentUrl: String): String =
    s"$LoginUrl&redirect_uri=$ApiUrl/$redirectTo?source_url=$currentUrl"

  def loginWithJoinUrl(currentUrl: String, eventId: EventId): String =
    loginWithRedirectUrl(s"github/withJoin/${eventId.value}", currentUrl)

  def loginWithFlagUrl(currentUrl: String, eventId: EventId): String =
    loginWithRedirectUrl(s"github/withFlag/${eventId.value}", currentUrl)

  def buildUserAvatarUrl(userId: UserId, size: Int): String =
    s"https://avatars.githubusercontent.com/u/${userId.value}?v=3&s=$size"

  def buildUserPageUrl(login: String): String = s"$RootPath/$login"
}
