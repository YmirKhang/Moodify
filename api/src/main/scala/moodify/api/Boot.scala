package moodify.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path, pathEndOrSingleSlash, _}
import akka.stream.ActorMaterializer
import moodify.Config
import moodify.core.{Identification, Insight}
import moodify.model.SimpleArtistProtocol._
import moodify.model.SimpleTrackProtocol._
import moodify.model.TrendlineProtocol._
import moodify.model.{Response, TimeRange}
import moodify.service.SpotifyService
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

object Boot extends Config {

  implicit val system: akka.actor.ActorSystem = ActorSystem("Moodify")
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit def listJsonWriter[T: JsonWriter]: RootJsonWriter[List[T]] = (list: List[T]) => JsArray(list.map(_.toJson).toVector)

  def main(args: Array[String]): Unit = {

    val routes = {
      /*
       * GET /
       * Root endpoint.
       */
      path("") {
        get {
          complete(Response.success())
        }
      } ~
        /*
         * GET /health
         * Get heartbeat of API.
         */
        path("health") {
          pathEndOrSingleSlash {
            get {
              complete(Response.success())
            }
          }
        } ~
        /*
         * GET /authenticate/user/{user-id}/code/{code}
         * Authenticate given user with given Spotify code.
         */
        pathPrefix("authenticate" / "user" / Segment / "code" / Segment) { (userId: String, code: String) =>
          pathEndOrSingleSlash {
            get {
              val success = Identification.authenticate(userId, code)
              complete(Response.json(success))
            }
          }
        } ~
        /*
         * GET /user/{user-id}
         * Endpoints with Authorization.
         */
        pathPrefix("user" / Segment) { userId: String =>
          val maybeAccessToken = Identification.authorize(userId)
          validate(maybeAccessToken.isDefined, Response.error("User is not authorized.")) {
            val accessToken = maybeAccessToken.get
            val spotify = new SpotifyService
            spotify.authorize(accessToken)
            /*
             * GET /user/{user-id}/trendline/{num-tracks}
             * Get trendline for given user using given number of tracks.
             */
            pathPrefix("trendline" / Segment) { numTracksString: String =>
              pathEndOrSingleSlash {
                get {
                  val numTracks = numTracksString.toInt
                  validate(numTracks <= 50, Response.json(success = false, message = "Limit is 50 songs.")) {
                    val insight = new Insight(spotify, userId)
                    val trendline = insight.getTrendline(numTracks)
                    complete(Response.json(success = true, data = trendline.toJson))
                  }
                }
              }
            }
            /*
             * GET /user/{user-id}/top-artists/{time-range}
             * Get top artists of given user for given time range.
             */
            pathPrefix("top-artists" / Segment) { timeRangeString: String =>
              pathEndOrSingleSlash {
                get {
                  val maybeTimeRange = Try(TimeRange.withName(timeRangeString)).toOption
                  validate(maybeTimeRange.isDefined, Response.error("Given time range is not valid.")) {
                    val insight = new Insight(spotify, userId)
                    val simpleArtistList = insight.getTopArtists(maybeTimeRange.get, TOP_ARTIST_TRACK_LIMIT)
                    complete(Response.json(success = true, data = simpleArtistList.toJson))
                  }
                }
              }
            }
            /*
             * GET /user/{user-id}/top-tracks/{time-range}
             * Get top tracks of given user for given time range.
             */
            pathPrefix("top-tracks" / Segment) { timeRangeString: String =>
              pathEndOrSingleSlash {
                get {
                  val maybeTimeRange = Try(TimeRange.withName(timeRangeString)).toOption
                  validate(maybeTimeRange.isDefined, Response.error("Given time range is not valid.")) {
                    val insight = new Insight(spotify, userId)
                    val simpleTrackList = insight.getTopTracks(maybeTimeRange.get, TOP_ARTIST_TRACK_LIMIT)
                    complete(Response.json(success = true, data = simpleTrackList.toJson))
                  }
                }
              }
            }
          }
        }
    }

    Http().bindAndHandle(routes, HTTP_INTERFACE, HTTP_PORT)
  }

}
