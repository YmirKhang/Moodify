package moodify.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path, pathEndOrSingleSlash, _}
import akka.stream.ActorMaterializer
import moodify.Config
import moodify.core.Identification
import moodify.model.Response

import scala.concurrent.ExecutionContextExecutor

object Boot extends Config {

  implicit val system: akka.actor.ActorSystem = ActorSystem("Moodify")
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val routes = {
      /*
        * GET /
        * Root endpoint.
        */
      path("") {
        get {
          val response = new Response(success = true)
          complete(response.toJson)
        }
      } ~
        /*
          * GET /health
          * Get heartbeat of API.
          */
        path("health") {
          pathEndOrSingleSlash {
            get {
              val response = new Response(success = true)
              complete(response.toJson)
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
              val response = new Response(success)
              complete(response.toJson)
            }
          }
        }
    }

    Http().bindAndHandle(routes, HTTP_INTERFACE, HTTP_PORT)
  }

}
