package moodify.model

import spray.json._

case class Trendline(
                      acousticness: Double = 0,
                      instrumentalness: Double = 0,
                      speechiness: Double = 0,
                      danceability: Double = 0,
                      liveness: Double = 0,
                      energy: Double = 0,
                      valence: Double = 0,
                      tempo: Double = 0,
                      loudness: Double = 0,
                      duration: Double = 0
                    )

object TrendlineProtocol extends DefaultJsonProtocol {
  implicit val trendlineFormat: RootJsonFormat[Trendline] = jsonFormat10(Trendline)
}
