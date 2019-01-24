package moodify.core

import com.typesafe.scalalogging.LazyLogging
import moodify.helper.DescriptionBuilder
import moodify.model.RecommendationPreferences
import moodify.repository.{TrackRepository, UserRepository}
import moodify.service.SpotifyService

import scala.io.Source
import scala.util.Random

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
    * Maximum seeds that can be used for recommendations, set by Spotify.
    */
  private val seedLimit = 5

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
      val enrichedPreferences = enrichPreferences(preferences)
      val recommendedTracks = spotify.getRecommendations(enrichedPreferences, limit, maybeMarket)
      val recommendedTracksUriArray = recommendedTracks.map(track => track.getUri)
      val description = DescriptionBuilder.getDescription(enrichedPreferences)
      val playlistId = prepareFreshPlaylist(description)
      spotify.addTracksToPlaylist(playlistId, recommendedTracksUriArray)
      true
    }
    catch {
      case exception: Throwable =>
        logger.error(exception.getMessage)
        logger.error(exception.getStackTrace.toList.toString)
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
        spotify.changePlaylistCoverImage(playlist.getId, getCoverImage)
        playlist.getId
      }
    }

    playlistId
  }

  /**
    * Enriches given recommendation preferences for user by filling up till 5 seed preference, if missing.
    *
    * @param preferences RecommendationPreferences
    * @return Enriched RecommendationPreferences
    */
  private def enrichPreferences(preferences: RecommendationPreferences): RecommendationPreferences = {
    val seedArtistsIdList = preferences.seedArtistIdList.getOrElse(List[String]())
    val seedTrackIdList = preferences.seedTrackIdList.getOrElse(List[String]())
    val numSeeds = seedArtistsIdList.length + seedTrackIdList.length

    if (numSeeds < seedLimit) {
      val necessaryArtistCount = seedLimit - numSeeds
      // Get artist id list by provided seeds. Prefer artist seed.
      val artistIdList = if (seedArtistsIdList.nonEmpty) {
        seedArtistsIdList
      } else {
        seedTrackIdList.map(trackId => TrackRepository.getSimpleTrack(trackId).artists.head.id)
      }
      // Find related artists and add them to the preferences by picking randomly.
      val artists = artistIdList.flatMap(artistId => spotify.getRelatedArtists(artistId))
      val candidates = artists.filterNot(artist => seedArtistsIdList.contains(artist.getId))
      val pickedArtists = Random.shuffle(candidates).take(necessaryArtistCount)
      val pickedArtistIdList = pickedArtists.map(artist => artist.getId)

      preferences.seedArtistIdList = Some(seedArtistsIdList.union(pickedArtistIdList))
    }

    preferences
  }

  /**
    * Get cover image for playlist.
    *
    * @return Cover image encoded in Base64 format.
    */
  private def getCoverImage: String = {
    try {
      val fileStream = getClass.getResourceAsStream("/cover-image.b64")
      Source.fromInputStream(fileStream).getLines.toList.head
    } catch {
      case _: Throwable => ""
    }
  }

}
