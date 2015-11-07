package io.scalac.octopus.client.config

import scalac.octopusonwire.shared.domain.{UserId, EventId}

object Github {
  val AuthorizeUrl = "https://github.com/login/oauth/authorize"
  val ClientId = "23d0d2f1f1cad53fd5f8"
  def LoginWithJoinUrl(currentUrl: String, eventId: EventId): String =
    s"${Github.AuthorizeUrl}?client_id=${Github.ClientId}&redirect_uri=${ClientConfig.ApiUrl}/github/withJoin/${eventId.value}?source_url=$currentUrl"
  def buildUserAvatarUrl(userId: UserId, size: Int) = s"https://avatars.githubusercontent.com/u/${userId.value}?v=3&s=$size"
}
