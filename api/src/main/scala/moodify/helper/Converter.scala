package moodify.helper

import moodify.model.Trendline

object Converter {

  /**
    * Convert given Trendline object to Map.
    *
    * @param trendline Trendline
    * @return Map[String, String]
    */
  def trendlineToMap(trendline: Trendline): Map[String, String] = {
    Map(
      "acousticness" -> trendline.acousticness.toString,
      "instrumentalness" -> trendline.instrumentalness.toString,
      "speechiness" -> trendline.speechiness.toString,
      "danceability" -> trendline.danceability.toString,
      "liveness" -> trendline.liveness.toString,
      "energy" -> trendline.energy.toString,
      "valence" -> trendline.valence.toString
    )
  }

  /**
    * Convert given Map to Trendline object.
    *
    * @param map Map[String, String]
    * @return Trendline
    */
  def mapToTrendline(map: Map[String, String]): Trendline = {
    Trendline(
      map("acousticness").toDouble,
      map("instrumentalness").toDouble,
      map("speechiness").toDouble,
      map("danceability").toDouble,
      map("liveness").toDouble,
      map("energy").toDouble,
      map("valence").toDouble
    )
  }

}
