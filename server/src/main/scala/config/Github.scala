package config
import concurrent.duration._

object Github {
  val ApiBaseUrl = "https://api.github.com"
  val ClientId = "ab13541e20f48a23d7a6" //"23d0d2f1f1cad53fd5f8"
  val ClientSecret = "b720b4c8ec0ca4142b00b1b56a81ac790b7c7e72" //"0c9a02311384ca90b99bdb74692bc3fe37238d9b"
  val AccessTokenUrl = "https://github.com/login/oauth/access_token"
  val AccessTokenKey = "access_token"
  val UserUrl = ApiBaseUrl + "/user"
  val UserFollowingUrl = UserUrl + "/following"
  val ApiRequestTimeout = 5.seconds
}