package moodify.core

import com.typesafe.scalalogging.LazyLogging
import moodify.model.RecommendationPreferences
import moodify.repository.{ArtistRepository, TrackRepository, UserRepository}
import moodify.service.SpotifyService

import scala.collection.mutable
import scala.io.Source

class Recommendation(spotify: SpotifyService, userId: String) extends LazyLogging {

  /**
    * Default name for newly created playlist.
    */
  private val playlistName = "Discover Moodify"

  /**
    * Default publicity setting for newly created playlist.
    */
  private val playlistPublicity = true

  /**
    * Creates a playlist with given preferences and limit for given user.
    *
    * @param preferences Preferences for recommended tracks.
    * @param limit       Number of tracks to recommend.
    * @return Success
    */
  def recommend(preferences: RecommendationPreferences, limit: Int): Boolean = {
    try {
      val userProfile = UserRepository.getUser(spotify, userId)
      val maybeMarket = Some(userProfile.countryCode)
      val recommendedTracks = spotify.getRecommendations(preferences, limit, maybeMarket)
      val recommendedTracksUriArray = recommendedTracks.map(track => track.getUri)
      val description = getDescription(preferences)
      val playlistId = prepareFreshPlaylist(description)
      spotify.addTracksToPlaylist(playlistId, recommendedTracksUriArray)
      true
    }
    catch {
      case exception: Throwable =>
        logger.error(exception.getMessage)
        false
    }
  }

  /**
    * Checks if a Moodify playlist is previously created.
    * If that is the case flushes the playlist, otherwise creates a new one.
    *
    * @param description Playlist description.
    * @return Spotify ID of playlist.
    */
  private def prepareFreshPlaylist(description: String): String = {
    val maybePlaylist = spotify.getCurrentUsersPlaylist(playlistName)

    val playlistId = {
      if (maybePlaylist.isDefined) {
        val playlistId = maybePlaylist.get.getId
        spotify.flushPlaylist(playlistId)
        spotify.changePlaylistsDescription(playlistId, description)
        playlistId
      } else {
        val playlist = spotify.createPlaylist(playlistName, description, playlistPublicity)
        val coverImage = Source.fromResource("cover-image.txt").getLines.toList.head
        spotify.changePlaylistCoverImage(playlist.getId, coverImage)
        playlist.getId
      }
    }

    playlistId
  }

  /**
    * Get description for playlist with given preferences.
    *
    * @param preferences Recommendation preferences.
    * @return Playlist description.
    */
  private def getDescription(preferences: RecommendationPreferences): String = {
    val descriptionBuilder = new StringBuilder("Highly personalized with Moodify based on ")
    val seedList = mutable.ListBuffer[String]()

    if (preferences.seedArtistIdList.isDefined) {
      val artistIdList = preferences.seedArtistIdList.get
      val artistNameList = artistIdList.map(artistId => ArtistRepository.getSimpleArtist(artistId).name)
      artistNameList.foreach(artist => seedList.append(artist))
    }

    if (preferences.seedTrackIdList.isDefined) {
      val seedTrackIdList = preferences.seedTrackIdList.get
      val trackNameList = seedTrackIdList.map(trackId => TrackRepository.getSimpleTrack(trackId).name)
      trackNameList.foreach(track => seedList.append(track))
    }

    val listSeparator = ", "
    var seedString = seedList.mkString("", listSeparator, ". ")
    if (seedList.length > 1) {
      val index = seedString.lastIndexOf(listSeparator)
      val (head, tail) = seedString.splitAt(index)
      val cleanTail = tail.replace(listSeparator, "")
      seedString = s"$head and $cleanTail"
    }

    descriptionBuilder.append(seedString)
    descriptionBuilder.append("Check https://moodify.app for more")
    val description = descriptionBuilder.toString

    description
  }

}
