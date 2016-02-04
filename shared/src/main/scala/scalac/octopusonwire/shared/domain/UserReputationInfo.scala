package scalac.octopusonwire.shared.domain

case class UserReputationInfo(userRep: Long, eventAddThreshold: Long){
  val canAddEvents = userRep >= eventAddThreshold
}

object TrustedReputationInfo extends UserReputationInfo(0, 0)