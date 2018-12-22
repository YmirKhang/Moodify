package moodify.helper

import akka.http.scaladsl.model.headers.RawHeader

object HTTPHelper {

  /**
    * Get response headers for given environment environment.
    *
    * @param environment Environment
    * @return Response headers
    */
  def getHeaders(environment: String): List[RawHeader] = environment match {
    case "TEST" =>
      List(
        RawHeader("Access-Control-Allow-Origin", "http://localhost:8000"),
        RawHeader("Access-Control-Allow-Methods", "GET")
      )
    case _ => List()
  }

}
