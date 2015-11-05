package config

object Github {
  val ApiBaseUrl = "https://api.github.com"
  val ClientId = "23d0d2f1f1cad53fd5f8"
  val ClientSecret = "0c9a02311384ca90b99bdb74692bc3fe37238d9b"
  val AccessTokenUrl = "https://github.com/login/oauth/access_token"
  val UserUrl = ApiBaseUrl + "/user"
  val AccessToken = "access_token"
  val ApiRequestTimeout = 5000
}