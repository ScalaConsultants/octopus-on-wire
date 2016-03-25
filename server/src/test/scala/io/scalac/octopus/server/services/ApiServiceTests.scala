package io.scalac.octopus.server.services

import config.ServerConfig.{DefaultReputation, ReputationRequiredToAddEvents}
import data._
import domain.UserIdentity
import io.scalac.octopus.server.OctoSpec
import io.scalac.octopus.server.TestData._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.{ApiService, GithubApi}
import tools.TimeHelpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder._
import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain.{UserInfo, _}

class ApiServiceTests extends OctoSpec{
  val validId = UserIdentity("some-token", UserId(1))
  val mockEventSource: EventSource = new InMemoryEventSource with MockedEvents

  val mockGithubApi = mock[GithubApi]

  def mockService: ApiService = new ApiService(Some(validId), mockEventSource, new InMemoryUserCache(mockGithubApi))

  "getUserInfo" should "fail for non-logged user" in {
    val apiService = new InMemoryApiService(None, mockGithubApi)

    apiService.getUserInfo().failed.futureValue shouldBe an[Exception]
  }

  it should "work for logged user" in {
    val userName = "Simple user"

    val mockGh = mock[GithubApi]

    when(mockGh.getUserInfo(validId.id, Some(validId.token))) thenReturnFuture UserInfo(UserId(1), userName)

    val apiService = new InMemoryApiService(
      Some(validId),
      mockGh
    )

    apiService.getUserInfo().futureValue shouldBe UserInfo(UserId(1), userName)
  }
  it should "fail if token is wrong" in {
    val mockGh = mock[GithubApi]

    when(mockGh.getUserInfo(UserId(1), Some("bad-token")))
      .thenReturn(Future.failed(new Exception("User info not found")))

    val apiService = new InMemoryApiService(
      Some(UserIdentity("bad-token", UserId(1))),
      mockGh
    )

    apiService.getUserInfo().failed.futureValue shouldBe an[Exception]
  }

  "getUserEventInfo" should "fail for nonexistent event" in {
    val apiService = new InMemoryApiService(None, mockGithubApi)
    apiService.getUserEventInfo(EventId(100)).failed.futureValue shouldBe an[Exception]
  }
  it should "work if the event exists" in {
    val apiService = new InMemoryApiService(None, mockGithubApi)
    apiService.getUserEventInfo(EventId(1)).futureValue shouldBe UserEventInfo(
      DummyData.events.head,
      userJoined = false,
      11,
      eventActive = true
    )
  }
  it should "work if the user is logged but didn't join" in {
    val apiService = new InMemoryApiService(Some(validId), mockGithubApi)

    apiService.getUserEventInfo(EventId(1)).futureValue shouldBe UserEventInfo(
      DummyData.events.head,
      userJoined = false,
      11,
      eventActive = true
    )
  }
  it should "work if the user has joined event" in {
    val apiService = new InMemoryApiService(Some(validId), mockGithubApi)
    whenReady(apiService.joinEventAndGetJoins(EventId(1))) { z =>
      apiService.getUserEventInfo(EventId(1)).futureValue shouldBe UserEventInfo(
        DummyData.events.head,
        userJoined = true,
        12,
        eventActive = true
      )
    }
  }
  it should "work if the user is not logged in" in {
    val apiService = new InMemoryApiService(None, mockGithubApi)
    whenReady(apiService.joinEventAndGetJoins(EventId(1))) { z =>
      apiService.getUserEventInfo(EventId(1)).futureValue shouldBe UserEventInfo(
        DummyData.events.head,
        userJoined = false,
        11,
        eventActive = true
      )
    }
  }

  "getFutureItems(3)" should "return first 3 of future existing items if user not logged in" in {

    val apiService = new ApiService(None, mockEventSource, new InMemoryUserCache(mockGithubApi))

    apiService.getFutureItems(3).futureValue.map(_.id.value) should contain theSameElementsAs List(2, 3, 4)
  }

  it should "skip one event if user has flagged one" in {
    whenReady(mockService.flagEvent(EventId(3))) { _ =>
      mockService.getFutureItems(3).futureValue.map(_.id.value) should contain theSameElementsAs List(2, 4, 5)
    }
  }

  "getEventsForRange(from = yesterday, to = today + days(4)" should "work well for non-logged user" in {
    val apiService = new ApiService(None, mockEventSource, new InMemoryUserCache(mockGithubApi))
    apiService.getEventsForRange(now - days(1), now + days(4)).futureValue.map(_.id.value) should contain theSameElementsAs List(1, 2)
  }

  it should "also work well for logged user with an event flagged" in {
    whenReady(mockService.flagEvent(EventId(2))) { _ =>
      mockService.getEventsForRange(now - days(1), now + days(4)).futureValue.map(_.id.value) should contain theSameElementsAs List(1)
    }
  }

  "joinEventAndGetJoins" must "not join a past event" in {
    (mockService joinEventAndGetJoins EventId(1)).futureValue shouldBe
      EventJoinInfo(11, TryingToJoinPastEvent.apply)
  }

  it should "not join a nonexistent event" in {
    (mockService joinEventAndGetJoins EventId(100)).futureValue shouldBe
      EventJoinInfo(0, EventNotFound.apply)
  }

  it should "join a future event" in {
    (mockService joinEventAndGetJoins EventId(2)).futureValue shouldBe EventJoinInfo(7, JoinSuccessful.apply)
  }

  it should "not join an event twice" in {
    whenReady(mockService joinEventAndGetJoins EventId(2)) { _ =>
      (mockService joinEventAndGetJoins EventId(2)).futureValue shouldBe EventJoinInfo(7, AlreadyJoined.apply)
    }
  }

  it should "not join an event if not authorized" in {
    val apiService = new InMemoryApiService(None, mockGithubApi)
    apiService.joinEventAndGetJoins(EventId(1)).futureValue shouldBe
      EventJoinInfo(11, UserNotFound.apply)
  }

  "getUsersJoined" should "contain as many friends as possible" in {
    val friends = Set(1136843, 1548278, 10749622, 192549, 13625545, 1097302, 82964, 345056, 390629, 4959786).map(UserId(_))
    val joins = Set(5664242).map(UserId(_)) ++ friends

    val uc = mock[UserCache]
    val es = mock[EventSource]
    when(es.getJoins(EventId(1))).thenReturnFuture(joins)
    when(uc.getOrFetchUserFriends(validId)).thenReturnFuture(friends)

    joins.foreach { id =>
      when(uc.getOrFetchUserInfo(id, Some(validId.token))).thenReturnFuture(UserInfo(id, "username"))
    }

    val api = new ApiService(Some(validId), es, uc)

    api.getUsersJoined(EventId(1), 10).futureValue shouldBe friends.map(UserInfo(_, "username"))
  }

  it should "contain nobody if there are no joins" in {
    val uc = mock[UserCache]
    val es = mock[EventSource]
    val api = new ApiService(Some(validId), es, uc)
    when(uc.getOrFetchUserFriends(validId)).thenReturnFuture(Set.empty[UserId])
    when(es.getJoins(EventId(1))).thenReturnFuture(Set.empty[UserId])

    api.getUsersJoined(EventId(1), 10).futureValue shouldBe empty
  }

  it should "contain some people if the user is not logged in" in {
    val uc = mock[UserCache]
    val es = mock[EventSource]
    val api = new ApiService(None, es, uc)
    val joins = Set(1, 2, 3).map(UserId(_))

    joins.foreach { id =>
      when(uc.getOrFetchUserInfo(id, None)).thenReturnFuture(UserInfo(id, "username"))
    }

    when(es.getJoins(EventId(1))) thenReturnFuture joins

    api.getUsersJoined(EventId(1), 10).futureValue should
      contain theSameElementsAs Set(1, 2, 3).map(id => UserInfo(UserId(id), "username"))
  }

  "addEvent" must "not add an event if user isn't logged in" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]
    (new ApiService(None, es, uc) addEvent getSampleValidEvent).futureValue shouldBe FailedToAdd(UserNotLoggedIn)
  }

  it must "not add an event that ends in the past" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(ReputationRequiredToAddEvents)
    when(uc.isUserTrusted(validId.id)).thenReturnFuture(false)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "some-user"))

    val api = new ApiService(Some(validId), es, uc)
    (api addEvent oldEvent).futureValue shouldBe FailedToAdd(EventCantEndInThePast)
  }

  it should "not add an event if user doesn't have X past items joined" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(DefaultReputation)
    when(uc.isUserTrusted(validId.id)).thenReturnFuture(false)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "some-user"))

    val api = new ApiService(Some(validId), es, uc)
    (api addEvent getSampleValidEvent).futureValue shouldBe FailedToAdd(UserCantAddEventsYet)
  }

  it should "add an event when logged in having X past events joined" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    val event = getSampleValidEvent

    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(ReputationRequiredToAddEvents)
    when(es.addEvent(event)).thenReturnFuture(Added())
    when(uc.isUserTrusted(validId.id)).thenReturnFuture(false)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "some-user"))

    val api = new ApiService(Some(validId), es, uc)

    (api addEvent event).futureValue shouldBe Added()
  }

  it should "add an event when user is trusted" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    val event = getSampleValidEvent

    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(0)
    when(es.addEvent(event)).thenReturnFuture(Added())
    when(uc.isUserTrusted(validId.id)).thenReturnFuture(true)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "some-user"))

    val api = new ApiService(Some(validId), es, uc)

    (api addEvent event).futureValue shouldBe Added()
  }

  "getUserReputation" should "throw exception if user not logged in" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]
    new ApiService(None, es, uc).getUserReputation().failed.futureValue shouldBe an[Exception]
  }

  it should "work for logged in user" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    when(uc.isUserTrusted(validId.id)).thenReturnFuture(false)
    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(0)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "username"))
    new ApiService(Some(validId), es, uc).getUserReputation().futureValue shouldBe
      UserReputationInfo("username", DefaultReputation, ReputationRequiredToAddEvents)
  }

  it should "work for trusted user" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    when(uc.isUserTrusted(validId.id)).thenReturnFuture(true)
    when(es.countPastJoinsBy(validId.id)).thenReturnFuture(0)
    when(uc.getUserInfo(validId.id)).thenReturnFuture(UserInfo(validId.id, "username"))
    new ApiService(Some(validId), es, uc).getUserReputation().futureValue shouldBe
      UserReputationInfo("username", ReputationRequiredToAddEvents + DefaultReputation, ReputationRequiredToAddEvents)
  }

  "flagEvent" should "not work for logged user" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]
    new ApiService(None, es, uc).flagEvent(EventId(1)).futureValue shouldBe false
  }

  it should "not work if user has already flagged event" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]

    when(es.addFlag(EventId(1), validId.id)).thenReturnFuture(false)
    new ApiService(Some(validId), es, uc).flagEvent(EventId(1)).futureValue shouldBe false
  }

  it should "work if the user is logged in" in {
    val es = mock[EventSource]
    val uc = mock[UserCache]
    when(es.addFlag(EventId(1), validId.id)).thenReturnFuture(true)

    new ApiService(Some(validId), es, uc).flagEvent(EventId(1)).futureValue shouldBe true
  }
}

class InMemoryApiService(userIdentity: Option[UserIdentity], githubApi: GithubApi)
  extends ApiService(userIdentity, new InMemoryEventSource, new InMemoryUserCache(githubApi))

trait MockedEvents {
  self: InMemoryEventSource =>
  events = List(
    Event(EventId(1), "Warsaw Scala FortyFives - Scala Application Development #scala45pl", now - hours(2), now - hours(1), 3600000, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
    Event(EventId(2), "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", now + days(3) + hours(4), now + days(3) + hours(12), 3600000, "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
    Event(EventId(3), "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", now + days(6), now + days(6) + hours(8), 3600000, "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
    Event(EventId(4), "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", now + days(8), now + days(8) + hours(8), 3600000, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
    Event(EventId(5), "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", now + days(10), now + days(10) + hours(8), 3600000, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/")
  )
}