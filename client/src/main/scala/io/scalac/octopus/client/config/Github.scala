package io.scalac.octopus.client.config

object Github {
  val AuthorizeUrl = "https://github.com/login/oauth/authorize"
  val ClientId = "23d0d2f1f1cad53fd5f8"
  val ClientSecret = "0c9a02311384ca90b99bdb74692bc3fe37238d9b"
  def LoginWithJoinUrl(currentUrl: String, eventId: Long): String =
    s"${Github.AuthorizeUrl}?client_id=${Github.ClientId}&redirect_uri=${ClientConfig.ApiUrl}/github/$eventId?source_url=$currentUrl"
}
