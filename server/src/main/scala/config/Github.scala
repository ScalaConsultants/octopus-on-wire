package config

object Github {
  val ApiBaseUrl = "https://api.github.com"
  val ClientId = "23d0d2f1f1cad53fd5f8"
  val ClientIdKey = "client_id"
  val ClientSecret = "0c9a02311384ca90b99bdb74692bc3fe37238d9b"
  val ClientSecretKey = "client_secret"
  val AccessTokenUrl = "https://github.com/login/oauth/access_token"
  val AccessTokenKey = "access_token"
  val UserUrl = ApiBaseUrl + "/user"
  val UserFollowingUrl = UserUrl + "/following"
  val ApiRequestTimeout = 5000
}