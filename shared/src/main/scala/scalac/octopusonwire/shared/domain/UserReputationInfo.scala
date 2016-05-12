package scalac.octopusonwire.shared.domain

case class UserReputationInfo(userLogin: String, userRep: Long, eventAddThreshold: Long) {
  val canAddEvents = userRep >= eventAddThreshold
}