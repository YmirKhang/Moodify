package moodify.core

import com.typesafe.scalalogging.LazyLogging
import moodify.model.RecommendationPreferences
import moodify.repository.UserRepository
import moodify.service.SpotifyService

class Recommendation(spotifyService: SpotifyService, userId: String) extends LazyLogging {

  /**
    * Default name for newly created playlist.
    */
  private val playlistName = "Discover Moodify"

  /**
    * Default description for newly created playlist.
    */
  private val playlistDescription = "Highly personalized with Moodify. Check https://moodify.app for more"

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
      val market = UserRepository.getCountryCode(spotifyService, userId)
      val recommendedTracks = spotifyService.getRecommendations(preferences, limit, Some(market))
      val recommendedTracksUriArray = recommendedTracks.map(track => track.getUri)
      val playlistId = prepareFreshPlaylist()
      spotifyService.addTracksToPlaylist(playlistId, recommendedTracksUriArray)
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
    * @return Spotify ID of playlist.
    */
  private def prepareFreshPlaylist(): String = {
    val maybePlaylist = spotifyService.getCurrentUsersPlaylist(playlistName)

    val playlistId = {
      if (maybePlaylist.isDefined) {
        val playlistId = maybePlaylist.get.getId
        spotifyService.flushPlaylist(playlistId)
        spotifyService.changePlaylistsDescription(playlistId, playlistDescription)
        playlistId
      } else {
        val playlist = spotifyService.createPlaylist(playlistName, playlistDescription, playlistPublicity)
        playlist.getId
      }
    }

    playlistId
  }

}
