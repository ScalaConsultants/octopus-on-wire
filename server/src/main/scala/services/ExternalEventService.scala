package services

import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import services.MeetupConfig.ScalaMeetupUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{Event, EventId}

object MeetupConfig {
  val ApiKey = "6339662f6c7c347856e7cf7d101143"
  val BaseUrl = "https://api.meetup.com"
  val ScalaMeetupUrl = s"$BaseUrl/2/open_events.json?topic=scala&status=upcoming&key=$ApiKey"
}

trait ExternalEventService {
  def fetchEvents: Future[Seq[Event]]
}

object MeetupEventService extends ExternalEventService {
  override def fetchEvents  =
    WS.url(ScalaMeetupUrl).withRequestTimeout(10.seconds.toMillis).get.map { result =>
      val results = (result.json \ "results").as[Seq[JsValue]]

      results.flatMap { element =>
        for {
          name <- (element \ "name").asOpt[String]
          time <- (element \ "time").asOpt[Long]
          duration <- (element \ "duration").asOpt[Long]
          utcOffset <- (element \ "utc_offset").asOpt[Long]
          url <- (element \ "event_url").asOpt[String]
          venue <- (element \ "venue").asOpt[JsValue]
        } yield {
          val venueName = (venue \ "name").asOpt[String]
          val venueAddress = (venue \ "address_1").asOpt[String]
          val venueCity = (venue \ "city").asOpt[String]
          val location = List(venueName, venueAddress, venueCity).flatten.mkString(", ").take(100)

          Event(EventId(-1), name, time, time + duration, utcOffset, location, url)
        }
      }
    }
}