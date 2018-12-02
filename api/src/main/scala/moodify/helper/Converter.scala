package moodify.helper

import com.wrapper.spotify.model_objects.specification.Artist
import moodify.model.{SimpleArtist, Trendline}

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

  /**
    * Convert given SimpleArtist object to Map.
    *
    * @param simpleArtist SimpleArtist
    * @return Map[String, String]
    */
  def simpleArtistToMap(simpleArtist: SimpleArtist): Map[String, String] = {
    Map(
      "id" -> simpleArtist.id,
      "name" -> simpleArtist.name,
      "imageUrl" -> simpleArtist.imageUrl
    )
  }

  /**
    * Convert given Map to SimpleArtist object.
    *
    * @param map Map[String, String]
    * @return SimpleArtist
    */
  def mapToSimpleArtist(map: Map[String, String]): SimpleArtist = {
    SimpleArtist(
      map("id"),
      map("name"),
      map("imageUrl")
    )
  }

  /**
    * Convert given Artist object to SimpleArtist object.
    *
    * @param artist Artist
    * @return SimpleArtist
    */
  def artistToSimpleArtist(artist: Artist): SimpleArtist = {
    SimpleArtist(
      artist.getId,
      artist.getName,
      artist.getImages.head.getUrl
    )
  }

}
