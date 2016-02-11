package scalac.octopusonwire.shared.domain

case class UserReputationInfo(user: String, userRep: Long, eventAddThreshold: Long){
  val canAddEvents = userRep >= eventAddThreshold
}