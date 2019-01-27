package moodify.helper

import akka.http.scaladsl.model.headers.RawHeader
import moodify.Config
import moodify.enumeration.Environment

object HTTPHelper {

  /**
    * Get response headers for given environment environment.
    *
    * @param environment Environment
    * @return Response headers
    */
  def getHeaders(environment: Environment.Type): List[RawHeader] = {
    List(
      RawHeader("Access-Control-Allow-Origin", Config.CORS_ALLOWED_URL),
      RawHeader("Access-Control-Allow-Methods", "GET")
    )
  }

}
