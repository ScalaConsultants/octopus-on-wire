package services

import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import services.MeetupConfig.ScalaMeetupUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{Origin, Event, EventId}

object MeetupConfig {
  val ApiKey = "6339662f6c7c347856e7cf7d101143"
  val BaseUrl = "https://api.meetup.com"
  val ScalaMeetupUrl = s"$BaseUrl/2/open_events.json?topic=scala&status=upcoming&key=$ApiKey"
}

trait EventService {
  def fetchEvents: Future[Seq[Event]]
}

object MeetupEventService extends EventService {
  override def fetchEvents =
    WS.url(ScalaMeetupUrl).withRequestTimeout(10.seconds.toMillis).get.map { result =>
      val results = (result.json \ "results").as[Seq[JsValue]]

      results.flatMap { element =>
        for {
          id <- (element \ "id").asOpt[String]
          created <- (element \ "created").asOpt[Long]
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

          val origin = MeetupOrigin(Some(id), Some(created))

          Event(EventId(-1), name, time, time + duration, utcOffset, location, url, origin)
        }
      }
    }
}

class MeetupOrigin(id: Option[String], added: Option[Long]) extends Origin(id, added, Some(MeetupOrigin.url))

object MeetupOrigin {
  val url = "meetup.com"

  def apply(id: Option[String], added: Option[Long]) = new MeetupOrigin(id, added)

  def unapply(origin: Origin): Option[(Option[String], Option[Long])] = origin.from.filter(_ == url) match {
    case None => None
    case _ => Some((origin.id, origin.added))
  }
}