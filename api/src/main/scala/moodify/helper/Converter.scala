package moodify.helper

import com.neovisionaries.i18n.CountryCode
import com.wrapper.spotify.model_objects.specification.{Artist, ArtistSimplified, Track}
import moodify.model.{SimpleArtist, SimpleTrack, Trendline, UserProfile}
import moodify.repository.ArtistRepository

object Converter {

  /**
    * Separates the list items during serialization.
    */
  private val listSeparator = ","

  /**
    * Default string for fields that are not available.
    */
  private val notAvailable = "N/A"

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

  /**
    * Convert given ArtistSimplified object to SimpleArtist object.
    *
    * @param artist ArtistSimplified
    * @return SimpleArtist
    */
  def artistSimplifiedToSimpleArtist(artist: ArtistSimplified): SimpleArtist = {
    SimpleArtist(
      artist.getId,
      artist.getName,
      notAvailable
    )
  }

  /**
    * Convert given SimpleTrack object to Map.
    *
    * @param simpleTrack SimpleTrack
    * @return Map[String, String]
    */
  def simpleTrackToMap(simpleTrack: SimpleTrack): Map[String, String] = {
    Map(
      "id" -> simpleTrack.id,
      "name" -> simpleTrack.name,
      "imageUrl" -> simpleTrack.imageUrl,
      "artistIdList" -> simpleTrack.artists.map(artist => artist.id).mkString(listSeparator)
    )
  }

  /**
    * Convert given Map to SimpleTrack object.
    *
    * @param map Map[String, String]
    * @return SimpleTrack
    */
  def mapToSimpleTrack(map: Map[String, String]): SimpleTrack = {
    SimpleTrack(
      map("id"),
      map("name"),
      map("imageUrl"),
      map("artistIdList").split(listSeparator).map(artistId => ArtistRepository.getSimpleArtist(artistId)).toList
    )
  }

  /**
    * Convert given Track object to SimpleTrack object.
    *
    * @param track Track
    * @return SimpleTrack
    */
  def trackToSimpleTrack(track: Track): SimpleTrack = {
    SimpleTrack(
      track.getId,
      track.getName,
      track.getAlbum.getImages.head.getUrl,
      track.getArtists.map(artist => artistSimplifiedToSimpleArtist(artist)).toList
    )
  }

  /**
    * Convert given UserProfile object to Map.
    *
    * @param userProfile UserProfile
    * @return Map[String, String]
    */
  def userProfileToMap(userProfile: UserProfile): Map[String, String] = {
    Map(
      "username" -> userProfile.username,
      "name" -> userProfile.name,
      "imageUrl" -> userProfile.imageUrl,
      "countryCode" -> userProfile.countryCode.getAlpha2
    )
  }

  /**
    * Convert given Map to UserProfile object.
    *
    * @param map Map[String, String]
    * @return UserProfile
    */
  def mapToUserProfile(map: Map[String, String]): UserProfile = {
    UserProfile(
      map("username"),
      map("name"),
      map("imageUrl"),
      CountryCode.valueOf(map("countryCode"))
    )
  }

}
