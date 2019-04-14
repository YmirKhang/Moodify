package moodify.model

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class RecommendationPreferences(
                                      var seedArtistIdList: Option[List[String]],
                                      seedTrackIdList: Option[List[String]],
                                      acousticness: Option[Double],
                                      instrumentalness: Option[Double],
                                      speechiness: Option[Double],
                                      danceability: Option[Double],
                                      liveness: Option[Double],
                                      energy: Option[Double],
                                      valence: Option[Double],
                                      tempo: Option[Int],
                                      loudness: Option[Double],
                                      duration: Option[Int],
                                      popularity: Option[Int]
                                    )

object RecommendationPreferencesProtocol extends DefaultJsonProtocol {
  implicit val recommendationPreferencesFormat: RootJsonFormat[RecommendationPreferences] = jsonFormat13(RecommendationPreferences)
}
