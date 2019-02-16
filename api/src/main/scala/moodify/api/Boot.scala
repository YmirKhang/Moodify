package moodify.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, get, path, pathEndOrSingleSlash, _}
import akka.stream.ActorMaterializer
import moodify.Config._
import moodify.core.{Identification, Insight, Recommendation, Search}
import moodify.enumeration.TimeRange
import moodify.helper.HTTPHelper
import moodify.model.RecommendationPreferences
import moodify.model.RecommendationPreferencesProtocol._
import moodify.model.SearchResponseProtocol._
import moodify.model.SimpleArtistProtocol._
import moodify.model.SimpleTrackProtocol._
import moodify.model.TrendlineProtocol._
import moodify.model.UserProfileProtocol._
import moodify.repository.UserRepository
import moodify.service.SpotifyService
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

object Boot extends FailureHandling {

  implicit val system: akka.actor.ActorSystem = ActorSystem("Moodify")
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val headers: List[RawHeader] = HTTPHelper.getHeaders(ENVIRONMENT)

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
      } ~ respondWithHeaders(headers) {
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
           * GET /authenticate/user/{udid}/code/{code}
           * Authenticate given user with given Spotify code.
           */
          path("authenticate" / "user" / Segment / "code" / Segment) { (udid: String, code: String) =>
            pathEndOrSingleSlash {
              get {
                val success = Identification.authenticate(udid, code)
                if (success) {
                  val maybeAccessToken = Identification.authorize(udid)
                  if (maybeAccessToken.isDefined) {
                    val accessToken = maybeAccessToken.get
                    val spotify = new SpotifyService
                    spotify.authorize(accessToken)
                    val userId = spotify.getCurrentUserId
                    val profile = UserRepository.getUser(spotify, userId)
                    complete(Response.success(data = profile.toJson))
                  } else complete(Response.error())
                } else complete(Response.error())
              }
            }
          } ~
          /*
           * GET /user/{udid}
           * Endpoints with Authorization.
           */
          pathPrefix("user" / Segment) { udid: String =>
            parameter("userId".as[String]) { userIdParameter =>
              val authorizationErrorMessage = "User is not authorized."
              val maybeAccessToken = Identification.authorize(udid)

              validate(maybeAccessToken.isDefined, Response.error(authorizationErrorMessage)) {
                val userId = Identification.getUserId(udid).get

                validate(userId == userIdParameter, Response.error(authorizationErrorMessage)) {
                  val accessToken = maybeAccessToken.get
                  val spotify = new SpotifyService
                  spotify.authorize(accessToken)

                  /*
                   * GET /user/{udid}/profile
                   * Get profile information of given user.
                   */
                  path("profile") {
                    pathEndOrSingleSlash {
                      get {
                        val profile = UserRepository.getUser(spotify, userId)
                        complete(Response.success(data = profile.toJson))
                      }
                    }
                  } ~
                    /*
                     * GET /user/{udid}/trendline/{num-tracks}
                     * Get trendline for given user using given number of tracks.
                     */
                    path("trendline" / IntNumber) { numTracks: Int =>
                      pathEndOrSingleSlash {
                        get {
                          validate(numTracks <= SPOTIFY_REQUEST_TRACK_LIMIT,
                            Response.json(success = false, message = s"Limit is $SPOTIFY_REQUEST_TRACK_LIMIT songs.")) {
                            val insight = new Insight(spotify, userId)
                            val trendline = insight.getTrendline(numTracks)
                            complete(Response.success(data = trendline.toJson))
                          }
                        }
                      }
                    } ~
                    /*
                     * GET /user/{udid}/top-artists/{time-range}
                     * Get top artists of given user for given time range.
                     */
                    path("top-artists" / Segment) { timeRangeString: String =>
                      pathEndOrSingleSlash {
                        get {
                          val maybeTimeRange = Try(TimeRange.withName(timeRangeString)).toOption
                          validate(maybeTimeRange.isDefined, Response.error("Given time range is not valid.")) {
                            val insight = new Insight(spotify, userId)
                            val simpleArtistList = insight.getTopArtists(maybeTimeRange.get, TOP_ARTIST_TRACK_LIMIT)
                            complete(Response.success(data = simpleArtistList.toJson))
                          }
                        }
                      }
                    } ~
                    /*
                     * GET /user/{udid}/top-tracks/{time-range}
                     * Get top tracks of given user for given time range.
                     */
                    path("top-tracks" / Segment) { timeRangeString: String =>
                      pathEndOrSingleSlash {
                        get {
                          val maybeTimeRange = Try(TimeRange.withName(timeRangeString)).toOption
                          validate(maybeTimeRange.isDefined, Response.error("Given time range is not valid.")) {
                            val insight = new Insight(spotify, userId)
                            val simpleTrackList = insight.getTopTracks(maybeTimeRange.get, TOP_ARTIST_TRACK_LIMIT)
                            complete(Response.success(data = simpleTrackList.toJson))
                          }
                        }
                      }
                    } ~
                    /*
                       * GET /user/{udid}/default-artists
                       * Get default artist list for current user's recommendations.
                       */
                    path("default-artists") {
                      pathEndOrSingleSlash {
                        get {
                          val insight = new Insight(spotify, userId)
                          val artistList = insight.getRecentArtists
                          complete(Response.success(data = artistList.toJson))
                        }
                      }
                    } ~
                    /*
                     * POST /user/{udid}/recommendation
                     * Get recommendations for user with given settings.
                     */
                    path("recommendation") {
                      pathEndOrSingleSlash {
                        post {
                          entity(as[String]) { body =>
                            validate(body.nonEmpty, Response.error("Recommendation settings must be provided.")) {
                              val preferences = body.parseJson.convertTo[RecommendationPreferences]
                              val recommendation = new Recommendation(spotify, userId)
                              val success = recommendation.recommend(preferences, NEW_PLAYLIST_SIZE)
                              complete(Response.json(success = success))
                            }
                          }
                        }
                      }
                    } ~
                    /*
                     * POST /user/{udid}/search
                     * Get search result for given query.
                     */
                    path("search") {
                      parameter("query".as[String]) { query =>
                        pathEndOrSingleSlash {
                          get {
                            val types = "artist,track"
                            val result = Search.query(spotify, userId, query, types, SEARCH_ITEM_LIMIT)
                            complete(Response.success(data = result.toJson))
                          }
                        }
                      }
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
